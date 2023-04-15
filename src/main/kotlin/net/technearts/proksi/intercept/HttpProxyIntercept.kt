package net.technearts.proksi.intercept

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

/**
 * http interceptor
 * beforeConnect -> beforeRequest -> afterResponse
 */
open class HttpProxyIntercept {
    /**
     * Intercept before establishing a connection with the target server
     */
    @Throws(Exception::class)
    open fun beforeConnect(clientChannel: Channel?, pipeline: HttpProxyInterceptPipeline) {
        pipeline.beforeConnect(clientChannel)
    }

    /**
     * Intercept the request header from the proxy server to the target server
     */
    @Throws(Exception::class)
    open fun beforeRequest(
        clientChannel: Channel, httpRequest: HttpRequest,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.beforeRequest(clientChannel, httpRequest)
    }

    /**
     * Intercept the request body from the proxy server to the target server
     */
    @Throws(Exception::class)
    open fun beforeRequest(
        clientChannel: Channel, httpContent: HttpContent,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.beforeRequest(clientChannel, httpContent)
    }

    /**
     * Intercept the response header from the proxy server to the client
     */
    @Throws(Exception::class)
    open fun afterResponse(
        clientChannel: Channel?, proxyChannel: Channel?, httpResponse: HttpResponse?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
    }

    /**
     * Intercept the response body from the proxy server to the client
     */
    @Throws(Exception::class)
    open fun afterResponse(
        clientChannel: Channel?, proxyChannel: Channel?, httpContent: HttpContent?,
        pipeline: HttpProxyInterceptPipeline
    ) {
        pipeline.afterResponse(clientChannel, proxyChannel, httpContent)
    }
}
