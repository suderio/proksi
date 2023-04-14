package net.technearts.proksi.exception

import io.netty.channel.Channel

open class HttpProxyExceptionHandle {
    fun startCatch(e: Throwable) {
        e.printStackTrace()
    }

    @Throws(Exception::class)
    open fun beforeCatch(clientChannel: Channel?, cause: Throwable) {
        cause.printStackTrace()
    }

    @Throws(Exception::class)
    open fun afterCatch(clientChannel: Channel?, proxyChannel: Channel?, cause: Throwable) {
        cause.printStackTrace()
    }
}
