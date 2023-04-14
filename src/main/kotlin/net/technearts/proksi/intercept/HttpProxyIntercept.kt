package net.technearts.proksi.intercept

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

/**
 * http拦截器
 * beforeConnect -> beforeRequest -> afterResponse
 */
open class HttpProxyIntercept {
    /**
     * 在与目标服务器建立连接之前拦截
     */
    @Throws(Exception::class)
    open fun beforeConnect(clientChannel: Channel?, pipeline: HttpProxyInterceptPipeline) {
        pipeline.beforeConnect(clientChannel)
    }

    /**
     * 拦截代理服务器到目标服务器的请求头
     */
    @Throws(Exception::class)
    open fun beforeRequest(
        clientChannel: Channel, httpRequest: HttpRequest,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.beforeRequest(clientChannel, httpRequest)
    }

    /**
     * 拦截代理服务器到目标服务器的请求体
     */
    @Throws(Exception::class)
    open fun beforeRequest(
        clientChannel: Channel, httpContent: HttpContent,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.beforeRequest(clientChannel, httpContent)
    }

    /**
     * 拦截代理服务器到客户端的响应头
     */
    @Throws(Exception::class)
    open fun afterResponse(
        clientChannel: Channel?, proxyChannel: Channel?, httpResponse: HttpResponse?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    /**
     * 拦截代理服务器到客户端的响应体
     */
    @Throws(Exception::class)
    open fun afterResponse(
        clientChannel: Channel?, proxyChannel: Channel?, httpContent: HttpContent?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpContent)
    }
}
