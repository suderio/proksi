package net.technearts.proksi

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.util.HttpUtil.checkUrl

/**
 * @Description Implementation of request forwarding function
 */
object InterceptForwardHttpProxyServer {
    // curl -k -x 127.0.0.1:9999 https://www.baidu.com
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        HttpProxyServer()
            .serverConfig(config)
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(object : HttpProxyIntercept() {
                        @Throws(Exception::class)
                        override fun beforeRequest(
                            clientChannel: Channel, httpRequest: HttpRequest,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            //Requests matched to Baidu are forwarded to Taobao
                            if (checkUrl(httpRequest, "^www.baidu.com$")) {
                                pipeline.requestProto!!.host = "www.taobao.com"
                                pipeline.requestProto!!.port = 443
                                pipeline.requestProto!!.ssl = true
                                httpRequest.headers()[HttpHeaderNames.HOST] = "www.taobao.com"
                            }
                            pipeline.beforeRequest(clientChannel, httpRequest)
                        }
                    })
                }
            })
            .httpProxyExceptionHandle(object : HttpProxyExceptionHandle() {
                @Throws(Exception::class)
                override fun beforeCatch(clientChannel: Channel?, cause: Throwable) {
                    cause.printStackTrace()
                }

                @Throws(Exception::class)
                override fun afterCatch(clientChannel: Channel?, proxyChannel: Channel?, cause: Throwable) {
                    cause.printStackTrace()
                }
            })
            .start(9999)
    }
}
