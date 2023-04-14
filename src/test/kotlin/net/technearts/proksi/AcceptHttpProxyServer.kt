package net.technearts.proksi

import io.netty.channel.Channel
import io.netty.handler.codec.http.*
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.server.accept.HttpProxyAcceptHandler
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

object AcceptHttpProxyServer {
    private val CLIENT_LIMIT_MAP: MutableMap<String, Int> = ConcurrentHashMap()
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.httpProxyAcceptHandler = object : HttpProxyAcceptHandler {
            override fun onAccept(request: HttpRequest?, clientChannel: Channel?): Boolean {
                val ip = getClientIp(clientChannel)
                val count = CLIENT_LIMIT_MAP.getOrDefault(ip, 1)
                if (count > 5) {
                    val fullHttpResponse: FullHttpResponse =
                        DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN)
                    fullHttpResponse.content().writeBytes("<html><div>访问过于频繁！</div></html>".toByteArray())
                    clientChannel!!.writeAndFlush(fullHttpResponse)
                    return false
                }
                CLIENT_LIMIT_MAP[ip] = count + 1
                return true
            }

            override fun onClose(clientChannel: Channel?) {
                CLIENT_LIMIT_MAP.computeIfPresent(getClientIp(clientChannel)) { s: String?, count: Int ->
                    if (count > 0) {
                        return@computeIfPresent count - 1
                    }
                    count
                }
            }

            private fun getClientIp(clientChannel: Channel?): String {
                val inetSocketAddress = clientChannel!!.localAddress() as InetSocketAddress
                return inetSocketAddress.hostString
            }
        }
        HttpProxyServer()
            .serverConfig(config)
            .start(9999)
    }
}
