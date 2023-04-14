package net.technearts.proksi.proxy

import io.netty.handler.proxy.HttpProxyHandler
import io.netty.handler.proxy.ProxyHandler
import io.netty.handler.proxy.Socks4ProxyHandler
import io.netty.handler.proxy.Socks5ProxyHandler
import java.net.InetSocketAddress

object ProxyHandleFactory {
    fun build(config: ProxyConfig?): ProxyHandler? {
        var proxyHandler: ProxyHandler? = null
        if (config != null) {
            val isAuth = config.user != null && config.pwd != null
            val inetSocketAddress = InetSocketAddress(
                config.host,
                config.port
            )
            proxyHandler = when (config.proxyType) {
                ProxyType.HTTP -> if (isAuth) {
                    HttpProxyHandler(
                        inetSocketAddress,
                        config.user, config.pwd
                    )
                } else {
                    HttpProxyHandler(inetSocketAddress)
                }

                ProxyType.SOCKS4 -> Socks4ProxyHandler(inetSocketAddress)
                ProxyType.SOCKS5 -> if (isAuth) {
                    Socks5ProxyHandler(
                        inetSocketAddress,
                        config.user, config.pwd
                    )
                } else {
                    Socks5ProxyHandler(inetSocketAddress)
                }

                null -> TODO()
            }
        }
        return proxyHandler
    }
}
