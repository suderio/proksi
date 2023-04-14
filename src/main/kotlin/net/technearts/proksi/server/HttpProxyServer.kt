package net.technearts.proksi.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.concurrent.Future
import io.netty.util.internal.logging.InternalLoggerFactory
import net.technearts.proksi.crt.CertPool.clear
import net.technearts.proksi.crt.CertUtil.genKeyPair
import net.technearts.proksi.crt.CertUtil.getSubject
import net.technearts.proksi.crt.CertUtil.loadCert
import net.technearts.proksi.crt.CertUtil.loadPriKey
import net.technearts.proksi.exception.HttpProxyExceptionHandle
import net.technearts.proksi.handler.HttpProxyServerHandler
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.proxy.ProxyConfig
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch

class HttpProxyServer {
    private var caCertFactory: HttpProxyCACertFactory? = null
    private var serverConfig: HttpProxyServerConfig? = null
    private var proxyInterceptInitializer: HttpProxyInterceptInitializer? = null
    private var httpProxyExceptionHandle: HttpProxyExceptionHandle? = null
    private var proxyConfig: ProxyConfig? = null
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private fun init() {
        if (serverConfig == null) {
            serverConfig = HttpProxyServerConfig()
        }
        serverConfig!!.proxyLoopGroup = NioEventLoopGroup(serverConfig!!.proxyGroupThreads)
        val contextBuilder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
        // 设置ciphers用于改变 client hello 握手协议指纹
        if (serverConfig!!.ciphers != null) {
            contextBuilder.ciphers(serverConfig!!.ciphers)
        }
        try {
            serverConfig!!.clientSslCtx = contextBuilder.build()
            if (serverConfig!!.isHandleSsl) {
                val classLoader = Thread.currentThread().contextClassLoader
                val caCert: X509Certificate?
                val caPriKey: PrivateKey?
                if (caCertFactory == null) {
                    caCert = loadCert(classLoader.getResourceAsStream("ca.crt")!!)
                    caPriKey = loadPriKey(classLoader.getResourceAsStream("ca_private.der")!!)
                } else {
                    caCert = caCertFactory!!.cACert
                    caPriKey = caCertFactory!!.cAPriKey
                }
                //读取CA证书使用者信息
                serverConfig!!.issuer = getSubject(caCert!!)
                //读取CA证书有效时段(server证书有效期超出CA证书的，在手机上会提示证书不安全)
                serverConfig!!.caNotBefore = caCert.notBefore
                serverConfig!!.caNotAfter = caCert.notAfter
                //CA私钥用于给动态生成的网站SSL证书签证
                serverConfig!!.caPriKey = caPriKey
                //生产一对随机公私钥用于网站SSL证书动态创建
                val keyPair = genKeyPair()
                serverConfig!!.serverPriKey = keyPair.private
                serverConfig!!.serverPubKey = keyPair.public
            }
        } catch (e: Exception) {
            serverConfig!!.isHandleSsl = false
            log.warn("SSL init fail,cause:" + e.message)
        }
        if (proxyInterceptInitializer == null) {
            proxyInterceptInitializer = HttpProxyInterceptInitializer()
        }
        if (httpProxyExceptionHandle == null) {
            httpProxyExceptionHandle = HttpProxyExceptionHandle()
        }
    }

    fun serverConfig(serverConfig: HttpProxyServerConfig?): HttpProxyServer {
        this.serverConfig = serverConfig
        return this
    }

    fun proxyInterceptInitializer(
        proxyInterceptInitializer: HttpProxyInterceptInitializer?
    ): HttpProxyServer {
        this.proxyInterceptInitializer = proxyInterceptInitializer
        return this
    }

    fun httpProxyExceptionHandle(
        httpProxyExceptionHandle: HttpProxyExceptionHandle?
    ): HttpProxyServer {
        this.httpProxyExceptionHandle = httpProxyExceptionHandle
        return this
    }

    fun proxyConfig(proxyConfig: ProxyConfig?): HttpProxyServer {
        this.proxyConfig = proxyConfig
        return this
    }

    fun caCertFactory(caCertFactory: HttpProxyCACertFactory?): HttpProxyServer {
        this.caCertFactory = caCertFactory
        return this
    }

    fun start(port: Int) {
        start(null, port)
    }

    fun start(ip: String?, port: Int) {
        try {
            val channelFuture = doBind(ip, port)
            val latch = CountDownLatch(1)
            channelFuture.addListener { future: Future<in Void?> ->
                if (future.cause() != null) {
                    httpProxyExceptionHandle!!.startCatch(future.cause())
                }
                latch.countDown()
            }
            latch.await()
            channelFuture.channel().closeFuture().sync()
        } catch (e: Exception) {
            httpProxyExceptionHandle!!.startCatch(e)
        } finally {
            close()
        }
    }

    fun startAsync(port: Int): CompletionStage<Void?> {
        return startAsync(null, port)
    }

    fun startAsync(ip: String?, port: Int): CompletionStage<Void?> {
        val channelFuture = doBind(ip, port)
        val future = CompletableFuture<Void?>()
        channelFuture.addListener { start: Future<in Void?> ->
            if (start.isSuccess) {
                future.complete(null)
                shutdownHook()
            } else {
                future.completeExceptionally(start.cause())
                close()
            }
        }
        return future
    }

    private fun doBind(ip: String?, port: Int): ChannelFuture {
        init()
        bossGroup = NioEventLoopGroup(serverConfig!!.bossGroupThreads)
        workerGroup = NioEventLoopGroup(serverConfig!!.workerGroupThreads)
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java) //                .option(ChannelOption.SO_BACKLOG, 100)
            .handler(LoggingHandler(LogLevel.DEBUG))
            .childHandler(object : ChannelInitializer<Channel>() {
                @Throws(Exception::class)
                override fun initChannel(ch: Channel) {
                    ch.pipeline().addLast(
                        "httpCodec", HttpServerCodec(
                            serverConfig!!.maxInitialLineLength,
                            serverConfig!!.maxHeaderSize,
                            serverConfig!!.maxChunkSize
                        )
                    )
                    ch.pipeline().addLast(
                        "serverHandle",
                        HttpProxyServerHandler(
                            serverConfig!!, proxyInterceptInitializer!!, proxyConfig!!,
                            httpProxyExceptionHandle!!
                        )
                    )
                }
            })
        return if (ip == null) bootstrap.bind(port) else bootstrap.bind(ip, port)
    }

    /**
     * 释放资源
     */
    fun close() {
        val eventLoopGroup = serverConfig!!.proxyLoopGroup
        if (!(eventLoopGroup!!.isShutdown || eventLoopGroup.isShuttingDown)) {
            eventLoopGroup.shutdownGracefully()
        }
        if (!(bossGroup!!.isShutdown || bossGroup!!.isShuttingDown)) {
            bossGroup!!.shutdownGracefully()
        }
        if (!(workerGroup!!.isShutdown || workerGroup!!.isShuttingDown)) {
            workerGroup!!.shutdownGracefully()
        }
        clear()
    }

    /**
     * 注册JVM关闭的钩子以释放资源
     */
    fun shutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread({ close() }, "Server Shutdown Thread"))
    }

    companion object {
        private val log = InternalLoggerFactory.getInstance(HttpProxyServer::class.java)

        //http代理隧道握手成功
        @JvmField
        val SUCCESS = HttpResponseStatus(
            200,
            "Connection established"
        )
        @JvmField
        val UNAUTHORIZED = HttpResponseStatus(
            407,
            "Unauthorized"
        )
    }
}
