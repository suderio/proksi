package net.technearts.proksi.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.ReferenceCountUtil

class HttpProxyClientHandler(private val clientChannel: Channel) : ChannelInboundHandlerAdapter() {
    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        //客户端channel已关闭则不转发了
        if (!clientChannel.isOpen) {
            ReferenceCountUtil.release(msg)
            return
        }
        val interceptPipeline = (clientChannel.pipeline()["serverHandle"] as HttpProxyServerHandler).interceptPipeline
        when (msg) {
            is HttpResponse -> {
                val decoderResult = msg.decoderResult()
                val cause = decoderResult.cause()
                if (cause != null) {
                    ReferenceCountUtil.release(msg)
                    exceptionCaught(ctx, cause)
                    return
                }
                interceptPipeline?.afterResponse(clientChannel, ctx.channel(), msg)
            }

            is HttpContent -> {
                interceptPipeline?.afterResponse(clientChannel, ctx.channel(), msg)
            }

            else -> {
                clientChannel.writeAndFlush(msg)
            }
        }
    }

    @Throws(Exception::class)
    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        ctx.channel().close()
    }

    @Deprecated("Deprecated in Java")
    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.channel().close()
        clientChannel.close()
        val exceptionHandle = (clientChannel.pipeline()["serverHandle"] as HttpProxyServerHandler).exceptionHandle
        exceptionHandle.afterCatch(clientChannel, ctx.channel(), cause)
    }
}
