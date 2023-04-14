package net.technearts.proksi

import io.netty.channel.Channel
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.server.HttpProxyServer

object NormalHttpProxyServer {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        HttpProxyServer()
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
