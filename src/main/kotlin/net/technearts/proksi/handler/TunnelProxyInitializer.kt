package net.technearts.proksi.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.handler.proxy.ProxyHandler

/**
 * Túnel de proxy HTTP, encaminhando pacotes originais
 */
class TunnelProxyInitializer(
    private val clientChannel: Channel,
    private val proxyHandler: ProxyHandler?
) : ChannelInitializer<Channel?>() {
    @Throws(Exception::class)
    protected override fun initChannel(ch: Channel?) {
        if (proxyHandler != null) {
            ch!!.pipeline().addLast(proxyHandler)
        }
        ch!!.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
            @Throws(Exception::class)
            override fun channelRead(ctx0: ChannelHandlerContext, msg0: Any) {
                clientChannel.writeAndFlush(msg0)
            }

            @Throws(Exception::class)
            override fun channelUnregistered(ctx0: ChannelHandlerContext) {
                ctx0.channel().close()
                clientChannel.close()
            }

            @Deprecated("Deprecated in Java")
            @Throws(Exception::class)
            override fun exceptionCaught(ctx0: ChannelHandlerContext, cause: Throwable) {
                ctx0.channel().close()
                clientChannel.close()
                val exceptionHandle =
                    (clientChannel.pipeline()["serverHandle"] as HttpProxyServerHandler).exceptionHandle
                exceptionHandle.afterCatch(clientChannel, ctx0.channel(), cause)
            }
        })
    }
}
