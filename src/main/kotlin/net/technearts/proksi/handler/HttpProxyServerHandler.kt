package net.technearts.proksi.handler

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.DecoderResult
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContextBuilder
import io.netty.resolver.NoopAddressResolverGroup
import io.netty.util.ReferenceCountUtil
import net.technearts.proksi.crt.CertPool.getCert
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.intercept.HttpProxyIntercept
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.proxy.ProxyConfig
import net.technearts.proksi.proxy.ProxyHandleFactory.build
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.server.auth.HttpAuthContext.setToken
import net.technearts.proksi.util.ProtoUtil
import net.technearts.proksi.util.ProtoUtil.getRequestProto
import java.net.InetSocketAddress
import java.net.URL
import java.util.*
import java.util.function.Consumer

class HttpProxyServerHandler(
    val serverConfig: HttpProxyServerConfig,
    val interceptInitializer: HttpProxyInterceptInitializer,
    private val proxyConfig: ProxyConfig,
    val exceptionHandle: HttpProxyExceptionHandle
) : ChannelInboundHandlerAdapter() {
    protected var channelFuture: ChannelFuture? = null

    //public ProxyConfig getProxyConfig() {
    //    return proxyConfig;
    //}
    protected var requestProto: ProtoUtil.RequestProto? = null
    protected var status = 0
    var interceptPipeline: HttpProxyInterceptPipeline? = null
        protected set
    protected var requestList: MutableList<Any>? = null
    protected var isConnect = false
    private var httpTagBuf: ByteArray? = null

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            val result: DecoderResult = msg.decoderResult()
            val cause = result.cause()
            if (cause is DecoderException) {
                status = 2
                var status: HttpResponseStatus? = null
                if (cause is TooLongHttpLineException) {
                    status = HttpResponseStatus.REQUEST_URI_TOO_LONG
                } else if (cause is TooLongHttpHeaderException) {
                    status = HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
                } else if (cause is TooLongHttpContentException) {
                    status = HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE
                }
                if (status == null) {
                    status = HttpResponseStatus.BAD_REQUEST
                }
                val response: HttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status)
                ctx.writeAndFlush(response)
                //ctx.channel().pipeline().remove("httpCodec");
                ReferenceCountUtil.release(msg)
                return
            }

            // The first time a connection is established, the host and port number are taken and the proxy handshake is processed.
            if (status == 0) {
                requestProto = getRequestProto(msg)
                if (requestProto == null) { // bad request
                    ctx.channel().close()
                    return
                }
                // 首次连接处理
                if (serverConfig.httpProxyAcceptHandler != null && !serverConfig.httpProxyAcceptHandler!!.onAccept(
                        msg,
                        ctx.channel()
                    )
                ) {
                    status = 2
                    ctx.channel().close()
                    return
                }
                // 代理身份验证
                if (!authenticate(ctx, msg)) {
                    status = 2
                    ctx.channel().close()
                    return
                }
                status = 1
                if (HttpMethod.CONNECT.name().equals(msg.method().name(), ignoreCase = true)) { // 建立代理握手
                    status = 2
                    val response: HttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpProxyServer.SUCCESS)
                    ctx.writeAndFlush(response)
                    ctx.channel().pipeline().remove("httpCodec")
                    // fix issue #42
                    ReferenceCountUtil.release(msg)
                    return
                }
            }
            interceptPipeline = buildPipeline()
            interceptPipeline!!.requestProto = requestProto!!.copy()
            // fix issue #27
            if (msg.uri().indexOf("/") != 0) {
                val url = URL(msg.uri())
                msg.setUri(url.file)
            }
            interceptPipeline!!.beforeRequest(ctx.channel(), msg)
            ReferenceCountUtil.release(msg)
        } else if (msg is HttpContent) {
            if (status != 2) {
                interceptPipeline!!.beforeRequest(ctx.channel(), msg)
            } else {
                ReferenceCountUtil.release(msg)
                status = 1
            }
        } else { // Processamento de handshake de ssl e websocket
            val byteBuf = msg as ByteBuf
            if (serverConfig.isHandleSsl && byteBuf.getByte(0).toInt() == 22) { // ssl握手
                requestProto!!.ssl = true
                val port = (ctx.channel().localAddress() as InetSocketAddress).port
                val sslCtx = SslContextBuilder.forServer(
                    serverConfig.serverPriKey,
                    getCert(port, requestProto!!.host, serverConfig)
                ).build()
                ctx.pipeline().addFirst(
                    "httpCodec", HttpServerCodec(
                        serverConfig.maxInitialLineLength, serverConfig.maxHeaderSize, serverConfig.maxChunkSize
                    )
                )
                ctx.pipeline().addFirst("sslHandle", sslCtx.newHandler(ctx.alloc()))
                // Percorra o pipeline novamente e obtenha a mensagem http descriptografada
                ctx.pipeline().fireChannelRead(msg)
                return
            }
            if (byteBuf.readableBytes() < 8) {
                httpTagBuf = ByteArray(byteBuf.readableBytes())
                byteBuf.readBytes(httpTagBuf)
                ReferenceCountUtil.release(msg)
                return
            }
            if (httpTagBuf != null) {
                val tmp = ByteArray(byteBuf.readableBytes())
                byteBuf.readBytes(tmp)
                byteBuf.writeBytes(httpTagBuf)
                byteBuf.writeBytes(tmp)
                httpTagBuf = null
            }

            // Se a mensagem HTTP estiver sendo executada atrás da conexão, ela também poderá capturar o pacote.
            if (isHttp(byteBuf)) {
                ctx.pipeline().addFirst(
                    "httpCodec", HttpServerCodec(
                        serverConfig.maxInitialLineLength, serverConfig.maxHeaderSize, serverConfig.maxChunkSize
                    )
                )
                ctx.pipeline().fireChannelRead(msg)
                return
            }
            handleProxyData(ctx.channel(), msg, false)
        }
    }

    private fun isHttp(byteBuf: ByteBuf): Boolean {
        val bytes = ByteArray(8)
        byteBuf.getBytes(0, bytes)
        val methodToken = String(bytes)
        return (methodToken.startsWith("GET ") || methodToken.startsWith("POST ") || methodToken.startsWith("HEAD ") || methodToken.startsWith(
            "PUT "
        ) || methodToken.startsWith("DELETE ") || methodToken.startsWith("OPTIONS ") || methodToken.startsWith("CONNECT ") || methodToken.startsWith(
            "TRACE "
        ))
    }

    @Throws(Exception::class)
    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        if (channelFuture != null) {
            channelFuture!!.channel().close()
        }
        ctx.channel().close()
        if (serverConfig.httpProxyAcceptHandler != null) {
            serverConfig.httpProxyAcceptHandler!!.onClose(ctx.channel())
        }
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (channelFuture != null) {
            channelFuture!!.channel().close()
        }
        ctx.channel().close()
        exceptionHandle.beforeCatch(ctx.channel(), cause)
    }

    private fun authenticate(ctx: ChannelHandlerContext, request: HttpRequest): Boolean {
        if (serverConfig.authenticationProvider != null) {
            val authProvider = serverConfig.authenticationProvider

            // Disable auth for request?
            if (!authProvider!!.matches(request)) {
                return true
            }
            val httpToken = authProvider.authenticate(request)
            if (httpToken == null) {
                val response: HttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpProxyServer.UNAUTHORIZED)
                response.headers()[HttpHeaderNames.PROXY_AUTHENTICATE] =
                    authProvider.authType() + " realm=\"" + authProvider.authRealm() + "\""
                ctx.writeAndFlush(response)
                return false
            }
            setToken(ctx.channel(), httpToken)
        }
        return true
    }

    @Throws(Exception::class)
    private fun handleProxyData(channel: Channel, msg: Any, isHttp: Boolean) {
        if (interceptPipeline == null) {
            interceptPipeline = buildOnlyConnectPipeline()
            interceptPipeline!!.requestProto = requestProto!!.copy()
        }
        val pipeRp = interceptPipeline!!.requestProto
        var isChangeRp = false
        if (isHttp && msg is HttpRequest) {
            // check if request modified
            if (pipeRp != requestProto) {
                isChangeRp = true
            }
        }
        if (isChangeRp || channelFuture == null) {
            // A conexão é anormal e o HttpContent entra, não é encaminhado
            if (isHttp && msg !is HttpRequest) {
                return
            }
            interceptPipeline!!.beforeConnect(channel)

            // by default, we use the proxy config set in the pipeline
            val proxyHandler =
                build(if (interceptPipeline!!.proxyConfig == null) proxyConfig else interceptPipeline!!.proxyConfig)
         /*
          * Adicione a extensão Server Name Indication (extensão SNI) do cliente SSL Hello. Alguns servidores são para clientes
          * Quando hello não tem a extensão SNI, ele retornará diretamente Alerta fatal recebido: handshake_failure (erro de aperto de mão)
          * Exemplo: https://cdn.mdn.mozilla.net/static/img/favicon32.7f3da72dcea1.png
          */
            val channelInitializer: ChannelInitializer<Channel?> = if (isHttp) HttpProxyInitializer(
                channel, pipeRp!!, proxyHandler
            ) else TunnelProxyInitializer(channel, proxyHandler)
            val bootstrap = Bootstrap()
            bootstrap.group(serverConfig.proxyLoopGroup) // Registrar pool de threads
                .channel(NioSocketChannel::class.java) // Use NioSocketChannel como a classe de canal para conexão
                .handler(channelInitializer)
            if (proxyHandler != null) {
                // O servidor proxy resolve o DNS e se conecta
                bootstrap.resolver(NoopAddressResolverGroup.INSTANCE)
            } else {
                bootstrap.resolver(serverConfig.resolver())
            }
            requestList = LinkedList()
            val channelFuture = bootstrap.connect(pipeRp!!.host, pipeRp.port)
            channelFuture.addListener(ChannelFutureListener { future: ChannelFuture ->
                if (future.isSuccess) {
                    future.channel().writeAndFlush(msg)
                    synchronized(requestList as LinkedList<Any>) {
                        (requestList as LinkedList<Any>).forEach(Consumer { obj: Any? ->
                            future.channel().writeAndFlush(obj)
                        })
                        (requestList as LinkedList<Any>).clear()
                        isConnect = true
                    }
                } else {
                    synchronized(requestList as LinkedList<Any>) {
                        (requestList as LinkedList<Any>).forEach(Consumer { msg: Any? -> ReferenceCountUtil.release(msg) })
                        (requestList as LinkedList<Any>).clear()
                    }
                    exceptionHandle.beforeCatch(channel, future.cause())
                    future.channel().close()
                    channel.close()
                }
            })
            this.channelFuture = channelFuture
        } else {
            synchronized(requestList!!) {
                if (isConnect) {
                    channelFuture!!.channel().writeAndFlush(msg)
                } else {
                    requestList!!.add(msg)
                }
            }
        }
    }

    private fun buildPipeline(): HttpProxyInterceptPipeline {
        val interceptPipeline = HttpProxyInterceptPipeline(object : HttpProxyIntercept() {
            @Throws(Exception::class)
            override fun beforeRequest(
                clientChannel: Channel, httpRequest: HttpRequest, pipeline: HttpProxyInterceptPipeline
            ) {
                handleProxyData(clientChannel, httpRequest, true)
            }

            @Throws(Exception::class)
            override fun beforeRequest(
                clientChannel: Channel, httpContent: HttpContent, pipeline: HttpProxyInterceptPipeline
            ) {
                handleProxyData(clientChannel, httpContent, true)
            }

            @Throws(Exception::class)
            override fun afterResponse(
                clientChannel: Channel?,
                proxyChannel: Channel?,
                httpResponse: HttpResponse?,
                pipeline: HttpProxyInterceptPipeline
            ) {
                clientChannel!!.writeAndFlush(httpResponse)
                if (HttpHeaderValues.WEBSOCKET.toString() == httpResponse!!.headers()[HttpHeaderNames.UPGRADE]) {
                    // websocket转发原始报文
                    proxyChannel!!.pipeline().remove("httpCodec")
                    clientChannel.pipeline().remove("httpCodec")
                }
            }

            @Throws(Exception::class)
            override fun afterResponse(
                clientChannel: Channel?,
                proxyChannel: Channel?,
                httpContent: HttpContent?,
                pipeline: HttpProxyInterceptPipeline
            ) {
                clientChannel!!.writeAndFlush(httpContent)
            }
        })
        interceptInitializer.init(interceptPipeline)
        return interceptPipeline
    }

    // fix issue #186: Quando não estiver interceptando mensagens https, exponha um ponto de extensão para configurações
    // de proxy e mantenha uma interface de programação consistente
    private fun buildOnlyConnectPipeline(): HttpProxyInterceptPipeline {
        val interceptPipeline = HttpProxyInterceptPipeline(HttpProxyIntercept())
        interceptInitializer.init(interceptPipeline)
        return interceptPipeline
    }
}
