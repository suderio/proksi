package net.technearts.proksi.server.accept

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest

interface HttpProxyAcceptHandler {
    /**
     * Triggered when a new connection is established by the client
     *
     * @param request
     * @param clientChannel
     * @return Returning true means release, returning false means disconnecting
     */
    fun onAccept(request: HttpRequest?, clientChannel: Channel?): Boolean

    /**
     * Fired when the client connection is closed
     *
     * @param clientChannel
     */
    fun onClose(clientChannel: Channel?)
}
