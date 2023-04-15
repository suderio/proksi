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
 * @Author: LiWei
 * @Description Redirect to the specified url when it matches the Baidu homepage
 * @Date: 2019/3/4 16:23
 */
object InterceptRedirectHttpProxyServer {
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
                            //Match to Baidu homepage and jump to Taobao
                            if (checkUrl(pipeline.httpRequest!!, "^www.baidu.com$")) {
                                val hookResponse: HttpResponse =
                                    DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                                hookResponse.setStatus(HttpResponseStatus.FOUND)
                                hookResponse.headers()[HttpHeaderNames.LOCATION] = "http://www.taobao.com"
                                clientChannel.writeAndFlush(hookResponse)
                                val lastContent: HttpContent = DefaultLastHttpContent()
                                clientChannel.writeAndFlush(lastContent)
                                return
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
