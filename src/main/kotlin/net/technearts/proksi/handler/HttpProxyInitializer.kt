package net.technearts.proksi.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.proxy.ProxyHandler
import net.technearts.proksi.util.ProtoUtil

/**
 * HTTP代理，转发解码后的HTTP报文
 */
class HttpProxyInitializer(
    private val clientChannel: Channel, private val requestProto: ProtoUtil.RequestProto,
    private val proxyHandler: ProxyHandler?
) : ChannelInitializer<Channel?>() {
    @Throws(Exception::class)
    protected override fun initChannel(ch: Channel?) {
        if (proxyHandler != null) {
            ch!!.pipeline().addLast(proxyHandler)
        }
        val serverConfig = (clientChannel.pipeline()["serverHandle"] as HttpProxyServerHandler).serverConfig
        if (requestProto.ssl) {
            ch!!.pipeline()
                .addLast(serverConfig.clientSslCtx!!.newHandler(ch.alloc(), requestProto.host, requestProto.port))
        }
        ch!!.pipeline().addLast(
            "httpCodec", HttpClientCodec(
                serverConfig.maxInitialLineLength,
                serverConfig.maxHeaderSize,
                serverConfig.maxChunkSize
            )
        )
        ch.pipeline().addLast("proxyClientHandle", HttpProxyClientHandler(clientChannel))
    }
}
