package net.technearts.proksi

import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import java.util.function.BiConsumer

/**
 * @author aomsweet
 */
object AsyncStartHttpProxyServer {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.bossGroupThreads = 1
        HttpProxyServer()
            .serverConfig(config)
            .startAsync(9999).whenComplete(BiConsumer { result: Void?, cause: Throwable? -> cause?.printStackTrace() })
    }
}
