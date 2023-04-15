package net.technearts.proksi.intercept.common

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.crt.CertUtil.loadCert
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.server.HttpProxyCACertFactory
import java.security.cert.X509Certificate

/**
 * Lidar com a página de download do certificado http://proxyServerIp:proxyServerPort
 */
class CertDownIntercept : HttpProxyIntercept {
    private var isDirect = false
    private var cert: X509Certificate? = null

    /**
     * Using proxyee's own CA certificate to construct CertDownIntercept
     */
    constructor()

    /**
     * Using CA public key in [CaCertFactory][HttpProxyCACertFactory] to construct CertDownIntercept
     *
     *  Visitors will receive a CA certificate from CaCertFactory instead of proxyee's built-in certificate.
     *
     * @param certFactory The same factory as [com.github.monkeywie.proxyee.server.HttpProxyServer]
     * ([caCertFactory(HttpProxyCACertFactory)][com.github.monkeywie.proxyee.server.HttpProxyServer])
     * is required, otherwise HTTP processing will fail
     * @throws Exception When factory throws an exception, it will be thrown directly.
     */
    constructor(certFactory: HttpProxyCACertFactory) {
        cert = certFactory.cACert
    }

    /**
     * Provide certificate structure CertDownIntercept directly.
     *
     * @param caCert The public key of CA certificate consistent with
     * [com.github.monkeywie.proxyee.server.HttpProxyServer].
     */
    constructor(caCert: X509Certificate?) {
        cert = caCert
    }

    @Throws(Exception::class)
    override fun beforeRequest(
        clientChannel: Channel, httpRequest: HttpRequest,
        pipeline: HttpProxyInterceptPipeline
    ) {
        val requestProto = pipeline.requestProto
        if (!requestProto!!.proxy) {
            isDirect = true
            if (httpRequest.uri().matches("^.*/ca.crt.*$".toRegex())) {  //下载证书
                val httpResponse: HttpResponse = DefaultHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK
                )
                val bts =
                    if (cert == null) loadCert(Thread.currentThread().contextClassLoader.getResourceAsStream("ca.crt")!!)
                        .encoded else cert!!.encoded
                httpResponse.headers()[HttpHeaderNames.CONTENT_TYPE] = "application/x-x509-ca-cert"
                httpResponse.headers()[HttpHeaderNames.CONTENT_LENGTH] = bts.size
                httpResponse.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
                val httpContent: HttpContent = DefaultLastHttpContent()
                httpContent.content().writeBytes(bts)
                clientChannel.writeAndFlush(httpResponse)
                clientChannel.writeAndFlush(httpContent)
                clientChannel.close()
            } else if (httpRequest.uri().matches("^.*/favicon.ico$".toRegex())) {
                clientChannel.close()
            } else {  //跳转下载页面
                val httpResponse: HttpResponse = DefaultHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK
                )
                val html =
                    "<html><body><div style=\"margin-top:100px;text-align:center;\"><a href=\"ca.crt\">ProxyeeRoot ca.crt</a></div></body></html>"
                httpResponse.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/html;charset=utf-8"
                httpResponse.headers()[HttpHeaderNames.CONTENT_LENGTH] = html.toByteArray().size
                httpResponse.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
                val httpContent: HttpContent = DefaultLastHttpContent()
                httpContent.content().writeBytes(html.toByteArray())
                clientChannel.writeAndFlush(httpResponse)
                clientChannel.writeAndFlush(httpContent)
            }
        } else {
            pipeline.beforeRequest(clientChannel, httpRequest)
        }
    }

    @Throws(Exception::class)
    override fun beforeRequest(
        clientChannel: Channel, httpContent: HttpContent,
        pipeline: HttpProxyInterceptPipeline
    ) {
        if (!isDirect) {
            pipeline.beforeRequest(clientChannel, httpContent)
        }
    }
}
