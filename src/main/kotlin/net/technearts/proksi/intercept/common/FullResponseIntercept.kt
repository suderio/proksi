package net.technearts.proksi.intercept.common

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline

abstract class FullResponseIntercept @JvmOverloads constructor(private val maxContentLength: Int = DEFAULT_MAX_CONTENT_LENGTH) :
    HttpProxyIntercept() {
    private var isMatch: Boolean? = null
    @Throws(Exception::class)
    override fun afterResponse(
        clientChannel: Channel?, proxyChannel: Channel?,
        httpResponse: HttpResponse?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        if (httpResponse is FullHttpResponse) {
            val fullHttpResponse = httpResponse
            // 判断是第一个处理FullResponse的拦截器是否匹配
            val isFirstMatch = isMatch != null && isMatch == true
            // 判断后续的拦截器是否匹配
            val isAfterMatch = if (isFirstMatch) false else match(pipeline.httpRequest, pipeline.httpResponse, pipeline)
            if (isFirstMatch || isAfterMatch) {
                handleResponse(pipeline.httpRequest, fullHttpResponse, pipeline)
                if (fullHttpResponse.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                    httpResponse.headers()[HttpHeaderNames.CONTENT_LENGTH] = fullHttpResponse.content().readableBytes()
                }
                if (pipeline.httpRequest is FullHttpRequest) {
                    val fullHttpRequest = pipeline.httpRequest as FullHttpRequest?
                    if (fullHttpRequest!!.content().refCnt() > 0) {
                        ReferenceCountUtil.release(fullHttpRequest)
                    }
                }
            }
            if (isFirstMatch) {
                proxyChannel!!.pipeline().remove("decompress")
                proxyChannel.pipeline().remove("aggregator")
            }
        } else {
            isMatch = match(pipeline.httpRequest, pipeline.httpResponse, pipeline)
            if (isMatch!!) {
                proxyChannel!!.pipeline().addAfter("httpCodec", "decompress", HttpContentDecompressor())
                proxyChannel.pipeline()
                    .addAfter("decompress", "aggregator", HttpObjectAggregator(maxContentLength))
                proxyChannel.pipeline().fireChannelRead(httpResponse)
                return
            }
        }
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    @Deprecated("")
    /**
     * 剥离到工具类中了：[net.technearts.proksi.util.HttpUtil.isHtml]
     */
    protected fun isHtml(httpRequest: HttpRequest, httpResponse: HttpResponse): Boolean {
        val accept = httpRequest.headers()[HttpHeaderNames.ACCEPT]
        val contentType = httpResponse.headers()[HttpHeaderNames.CONTENT_TYPE]
        return httpResponse.status().code() == 200 && accept != null && accept
            .matches("^.*text/html.*$".toRegex()) && contentType != null && contentType
            .matches("^text/html.*$".toRegex())
    }

    /**
     * 匹配到的响应会解码成FullResponse
     */
    abstract fun match(
        httpRequest: HttpRequest?, httpResponse: HttpResponse?,
        pipeline: HttpProxyInterceptPipeline?
    ): Boolean

    /**
     * 拦截并处理响应
     */
    open fun handleResponse(
        httpRequest: HttpRequest?, httpResponse: FullHttpResponse?,
        pipeline: HttpProxyInterceptPipeline?
    ) {
    }

    companion object {
        /**
         * default max content length size is 8MB
         */
        private const val DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024 * 8
    }
}
