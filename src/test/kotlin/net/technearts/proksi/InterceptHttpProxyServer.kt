package net.technearts.proksi

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.intercept.common.CertDownIntercept
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig

object InterceptHttpProxyServer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        HttpProxyServer()
            .serverConfig(config) //        .proxyConfig(new ProxyConfig(ProxyType.SOCKS5, "127.0.0.1", 1085))  //使用socks5二级代理
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(CertDownIntercept()) //处理证书下载
                    pipeline.addLast(object : HttpProxyIntercept() {
                        @Throws(Exception::class)
                        override fun beforeRequest(
                            clientChannel: Channel, httpRequest: HttpRequest,
                            pipeline: HttpProxyInterceptPipeline
                        ) {
                            //Replace UA and pretend to be a mobile browser
                            /*httpRequest.headers().set(HttpHeaderNames.USER_AGENT,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");*/
                            //转到下一个拦截器处理
                            pipeline.beforeRequest(clientChannel, httpRequest)
                        }

                        @Throws(Exception::class)
                        override fun afterResponse(
                            clientChannel: Channel?, proxyChannel: Channel?,
                            httpResponse: HttpResponse?, pipeline: HttpProxyInterceptPipeline
                        ) {

                            //Intercept the response, add a response header
                            httpResponse!!.headers().add("intercept", "test")
                            //Go to next interceptor processing
                            pipeline.afterResponse(clientChannel, proxyChannel, httpResponse)
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
