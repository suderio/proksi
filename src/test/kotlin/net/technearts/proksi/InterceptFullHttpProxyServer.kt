package net.technearts.proksi

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.intercept.common.CertDownIntercept
import net.technearts.proksi.intercept.common.FullRequestIntercept
import net.technearts.proksi.intercept.common.FullResponseIntercept
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import java.nio.charset.Charset

object InterceptFullHttpProxyServer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        HttpProxyServer()
            .serverConfig(config)
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(CertDownIntercept())
                    pipeline.addLast(object : FullRequestIntercept() {
                        override fun match(httpRequest: HttpRequest?, pipeline: HttpProxyInterceptPipeline?): Boolean {
                            return true
                        }
                    })
                    pipeline.addLast(object : FullResponseIntercept() {
                        override fun match(
                            httpRequest: HttpRequest?,
                            httpResponse: HttpResponse?,
                            pipeline: HttpProxyInterceptPipeline?
                        ): Boolean {
                            return true
                        }
                    })
                    pipeline.addLast(object : HttpProxyIntercept() {
                        private var fullHttpRequest: FullHttpRequest? = null
                        @Throws(Exception::class)
                        override fun beforeRequest(
                            clientChannel: Channel,
                            httpRequest: HttpRequest,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            val fullHttpRequest = httpRequest as FullHttpRequest?
                            this.fullHttpRequest = DefaultFullHttpRequest(
                                fullHttpRequest!!.protocolVersion(),
                                fullHttpRequest.method(),
                                fullHttpRequest.uri(),
                                fullHttpRequest.content().copy()
                            )
                            pipeline.beforeRequest(clientChannel, httpRequest as HttpRequest)
                        }

                        @Throws(Exception::class)
                        override fun afterResponse(
                            clientChannel: Channel?,
                            proxyChannel: Channel?,
                            httpResponse: HttpResponse?,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            println(fullHttpRequest.toString())
                            println(fullHttpRequest!!.content().toString(Charset.defaultCharset()))
                            fullHttpRequest!!.release()
                            val fullHttpResponse = httpResponse as FullHttpResponse?
                            println(fullHttpResponse.toString())
                            println(fullHttpResponse!!.content().toString(Charset.defaultCharset()))
                            pipeline.afterResponse(clientChannel, proxyChannel, httpResponse as HttpResponse)
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
            }
            )
            .start(9999)
    }
}
