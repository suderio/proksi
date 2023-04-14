package net.technearts.proksi

import io.netty.resolver.AddressResolver
import io.netty.resolver.AddressResolverGroup
import io.netty.resolver.DefaultNameResolver
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.Promise
import io.netty.util.internal.SocketUtils
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.*

object LoopbackHttpProxyServer {
    @JvmStatic
    fun main(args: Array<String>) {
        println("start loopback proxy server")
        val config = HttpProxyServerConfig(LoopbackAddressResolverGroup.INSTANCE)
        config.bossGroupThreads = 1
        config.workerGroupThreads = 1
        config.proxyGroupThreads = 1
        config.isHandleSsl = false
        HttpProxyServer()
            .serverConfig(config)
            .start(9999)
    }

    private class LoopbackAddressResolverGroup private constructor() : AddressResolverGroup<InetSocketAddress>() {
        override fun newResolver(executor: EventExecutor): AddressResolver<InetSocketAddress> {
            return LoopbackNameResolver(executor)
                .asAddressResolver()
        }

        private class LoopbackNameResolver(executor: EventExecutor?) : DefaultNameResolver(executor) {
            override fun doResolve(inetHost: String, promise: Promise<InetAddress>) {
                try {
                    promise.setSuccess(SocketUtils.addressByName("localhost"))
                } catch (unknownHostException: UnknownHostException) {
                    promise.setFailure(unknownHostException)
                }
            }

            override fun doResolveAll(inetHost: String, promise: Promise<List<InetAddress>>) {
                try {
                    promise.setSuccess(Arrays.asList(*SocketUtils.allAddressesByName("localhost")))
                } catch (unknownHostException: UnknownHostException) {
                    promise.setFailure(unknownHostException)
                }
            }
        }

        companion object {
            val INSTANCE = LoopbackAddressResolverGroup()
        }
    }
}
