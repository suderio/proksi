package net.technearts.proksi

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.server.auth.BasicHttpProxyAuthenticationProvider
import net.technearts.proksi.server.auth.HttpAuthContext.getToken
import net.technearts.proksi.server.auth.model.BasicHttpToken
import java.nio.charset.StandardCharsets

object AuthHttpProxyServer {
    // curl -i -x 127.0.0.1:9999 -U admin:123456 https://www.baidu.com
    // curl -v http://127.0.0.1:9999/status/health
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.authenticationProvider = object : BasicHttpProxyAuthenticationProvider() {
            override fun authenticate(usr: String?, pwd: String?): BasicHttpToken? {
                return if ("admin" == usr && "123456" == pwd) {
                    BasicHttpToken(usr, pwd)
                } else null
            }

            override fun matches(request: HttpRequest?): Boolean {
                return if (request!!.uri().matches("^/status/health$".toRegex())) {
                    false
                } else true
            }
        }
        HttpProxyServer()
            .serverConfig(config)
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(object : HttpProxyIntercept() {
                        private var isDirect = false
                        @Throws(Exception::class)
                        override fun beforeRequest(
                            clientChannel: Channel,
                            httpRequest: HttpRequest,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            val requestProto = pipeline.requestProto
                            if (!requestProto!!.proxy && httpRequest!!.uri().matches("^/status/health$".toRegex())) {
                                isDirect = true
                                val httpResponse: HttpResponse = DefaultHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.OK
                                )
                                val res = "OK"
                                val bts = res.toByteArray(StandardCharsets.UTF_8)
                                httpResponse.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/plain;charset=utf-8"
                                httpResponse.headers()[HttpHeaderNames.CONTENT_LENGTH] = bts.size
                                httpResponse.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
                                val httpContent: HttpContent = DefaultLastHttpContent()
                                httpContent.content().writeBytes(bts)
                                clientChannel!!.writeAndFlush(httpResponse)
                                clientChannel.writeAndFlush(httpContent)
                                clientChannel.close()
                            }
                        }

                        @Throws(Exception::class)
                        override fun beforeRequest(
                            clientChannel: Channel,
                            httpContent: HttpContent,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            if (!isDirect) pipeline.beforeRequest(clientChannel, httpContent)
                        }
                    })
                    pipeline.addLast(object : HttpProxyIntercept() {
                        @Throws(Exception::class)
                        override fun beforeConnect(clientChannel: Channel?, pipeline: HttpProxyInterceptPipeline) {
                            println(getToken(clientChannel))
                        }
                    })
                }
            })
            .start(9999)
    }
}
