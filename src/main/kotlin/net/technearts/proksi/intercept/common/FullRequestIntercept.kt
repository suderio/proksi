package net.technearts.proksi.intercept.common

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline

abstract class FullRequestIntercept @JvmOverloads constructor(private val maxContentLength: Int = DEFAULT_MAX_CONTENT_LENGTH) :
    HttpProxyIntercept() {
    @Throws(Exception::class)
    override fun beforeRequest(
        clientChannel: Channel,
        httpRequest: HttpRequest,
        pipeline: HttpProxyInterceptPipeline
    ) {
        if (httpRequest is FullHttpRequest) {
            handleRequest(httpRequest, pipeline)
            httpRequest.content().markReaderIndex()
            httpRequest.content().retain()
            if (httpRequest.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                httpRequest.headers()[HttpHeaderNames.CONTENT_LENGTH] = httpRequest.content().readableBytes()
            }
        } else if (match(httpRequest, pipeline)) {
            //重置拦截器
            pipeline.resetBeforeHead()
            //添加gzip解压处理
            clientChannel.pipeline().addAfter("httpCodec", "decompress", HttpContentDecompressor())
            //添加Full request解码器
            clientChannel.pipeline().addAfter("decompress", "aggregator", HttpObjectAggregator(maxContentLength))
            //重新过一遍处理器链
            clientChannel.pipeline().fireChannelRead(httpRequest)
            return
        }
        pipeline.beforeRequest(clientChannel, httpRequest)
    }

    @Throws(Exception::class)
    override fun afterResponse(
        clientChannel: Channel?,
        proxyChannel: Channel?,
        httpResponse: HttpResponse?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        //如果是FullHttpRequest
        if (pipeline.httpRequest is FullHttpRequest) {
            if (clientChannel!!.pipeline()["decompress"] != null) {
                clientChannel.pipeline().remove("decompress")
            }
            if (clientChannel.pipeline()["aggregator"] != null) {
                clientChannel.pipeline().remove("aggregator")
            }
            val httpRequest = pipeline.httpRequest as FullHttpRequest?
            httpRequest!!.content().resetReaderIndex()
        }
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    /**
     * 匹配到的请求会解码成FullRequest
     */
    abstract fun match(httpRequest: HttpRequest?, pipeline: HttpProxyInterceptPipeline?): Boolean

    /**
     * 拦截并处理响应
     */
    open fun handleRequest(httpRequest: FullHttpRequest?, pipeline: HttpProxyInterceptPipeline?) {}

    companion object {
        /**
         * default max content length size is 8MB
         */
        private const val DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024 * 8
    }
}
