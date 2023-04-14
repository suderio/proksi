package net.technearts.proksi

import io.netty.handler.codec.http.*
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.intercept.common.CertDownIntercept
import net.technearts.proksi.intercept.common.FullResponseIntercept
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.util.HttpUtil
import net.technearts.proksi.util.HttpUtil.checkUrl
import java.nio.charset.Charset

object InterceptFullResponseProxyServer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        // 设置Ciphers 用于改变 Client Hello 握手协议指纹
        val defaultCiphers: MutableSet<String> = LinkedHashSet()
        defaultCiphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256")
        defaultCiphers.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
        defaultCiphers.add("TLS_RSA_WITH_AES_128_CBC_SHA")
        defaultCiphers.add("TLS_RSA_WITH_AES_128_GCM_SHA256")
        defaultCiphers.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")
        config.ciphers = defaultCiphers
        HttpProxyServer()
            .serverConfig(config)
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(CertDownIntercept())
                    pipeline.addLast(object : FullResponseIntercept() {
                        override fun match(
                            httpRequest: HttpRequest?,
                            httpResponse: HttpResponse?,
                            pipeline: HttpProxyInterceptPipeline?
                        ): Boolean {
                            //在匹配到百度首页时插入js
                            return (checkUrl(pipeline!!.httpRequest!!, "^www.baidu.com$")
                                    && HttpUtil.isHtml(httpRequest!!, httpResponse!!))
                        }

                        override fun handleResponse(
                            httpRequest: HttpRequest?,
                            httpResponse: FullHttpResponse?,
                            pipeline: HttpProxyInterceptPipeline?
                        ) {
                            //打印原始响应信息
                            println(httpResponse.toString())
                            println(httpResponse?.content()?.toString(Charset.defaultCharset()))
                            //修改响应头和响应体
                            httpResponse?.headers()?.set("handel", "edit head")
                            /*int index = ByteUtil.findText(httpResponse.content(), "<head>");
                    ByteUtil.insertText(httpResponse.content(), index, "<script>alert(1)</script>");*/httpResponse?.content()
                                ?.writeBytes("<script>alert('hello proxyee')</script>".toByteArray())
                        }
                    })
                }
            })
            .start(9999)
    }
}
