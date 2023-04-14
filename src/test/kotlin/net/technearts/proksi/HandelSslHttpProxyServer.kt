package net.technearts.proksi

import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig

object HandelSslHttpProxyServer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        config.maxHeaderSize = 8192 * 2
        HttpProxyServer()
            .serverConfig(config)
            .start(9999)
    }
}
