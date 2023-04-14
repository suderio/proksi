package net.technearts.proksi.server

import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.http.HttpObjectDecoder
import io.netty.handler.ssl.SslContext
import io.netty.resolver.AddressResolverGroup
import io.netty.resolver.DefaultAddressResolverGroup
import net.technearts.proksi.server.accept.HttpProxyAcceptHandler
import net.technearts.proksi.server.auth.HttpProxyAuthenticationProvider
import java.net.SocketAddress
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

class HttpProxyServerConfig {
    var clientSslCtx: SslContext? = null
    var issuer: String? = null
    var caNotBefore: Date? = null
    var caNotAfter: Date? = null
    var caPriKey: PrivateKey? = null
    var serverPriKey: PrivateKey? = null
    var serverPubKey: PublicKey? = null
    var proxyLoopGroup: EventLoopGroup? = null
    var bossGroupThreads = 0
    var workerGroupThreads = 0
    var proxyGroupThreads = 0
    var isHandleSsl = false
    var httpProxyAcceptHandler: HttpProxyAcceptHandler? = null
    var authenticationProvider: HttpProxyAuthenticationProvider<*>? = null
    private val resolver: AddressResolverGroup<out SocketAddress?>
    var ciphers: Iterable<String>? = null
    var maxInitialLineLength = HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH
    var maxHeaderSize = HttpObjectDecoder.DEFAULT_MAX_HEADER_SIZE
    var maxChunkSize = HttpObjectDecoder.DEFAULT_MAX_CHUNK_SIZE

    @JvmOverloads
    constructor(resolver: AddressResolverGroup<out SocketAddress?> = DefaultAddressResolverGroup.INSTANCE) {
        this.resolver = resolver
    }

    private constructor(builder: Builder) {
        clientSslCtx = builder.clientSslCtx
        issuer = builder.issuer
        caNotBefore = builder.caNotBefore
        caNotAfter = builder.caNotAfter
        caPriKey = builder.caPriKey
        serverPriKey = builder.serverPriKey
        serverPubKey = builder.serverPubKey
        proxyLoopGroup = builder.proxyLoopGroup
        bossGroupThreads = builder.bossGroupThreads
        workerGroupThreads = builder.workerGroupThreads
        proxyGroupThreads = builder.proxyGroupThreads
        isHandleSsl = builder.handleSsl
        httpProxyAcceptHandler = builder.httpProxyAcceptHandler
        resolver = builder.resolver
        maxInitialLineLength = builder.maxInitialLineLength
        maxHeaderSize = builder.maxHeaderSize
        maxChunkSize = builder.maxChunkSize
    }

    fun resolver(): AddressResolverGroup<*> {
        return resolver
    }

    class Builder @JvmOverloads constructor(internal val resolver: AddressResolverGroup<out SocketAddress?> = DefaultAddressResolverGroup.INSTANCE) {
        internal var clientSslCtx: SslContext? = null
        internal var issuer: String? = null
        internal var caNotBefore: Date? = null
        internal var caNotAfter: Date? = null
        internal var caPriKey: PrivateKey? = null
        internal var serverPriKey: PrivateKey? = null
        internal var serverPubKey: PublicKey? = null
        internal var proxyLoopGroup: EventLoopGroup? = null
        internal var bossGroupThreads = 0
        internal var workerGroupThreads = 0
        internal var proxyGroupThreads = 0
        internal var handleSsl = false
        internal var httpProxyAcceptHandler: HttpProxyAcceptHandler? = null
        private var authenticationProvider: HttpProxyAuthenticationProvider<*>? = null
        internal var maxInitialLineLength = HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH
        internal var maxHeaderSize = HttpObjectDecoder.DEFAULT_MAX_HEADER_SIZE
        internal var maxChunkSize = HttpObjectDecoder.DEFAULT_MAX_CHUNK_SIZE
        fun setClientSslCtx(clientSslCtx: SslContext?): Builder {
            this.clientSslCtx = clientSslCtx
            return this
        }

        fun setIssuer(issuer: String?): Builder {
            this.issuer = issuer
            return this
        }

        fun setCaNotBefore(caNotBefore: Date?): Builder {
            this.caNotBefore = caNotBefore
            return this
        }

        fun setCaNotAfter(caNotAfter: Date?): Builder {
            this.caNotAfter = caNotAfter
            return this
        }

        fun setCaPriKey(caPriKey: PrivateKey?): Builder {
            this.caPriKey = caPriKey
            return this
        }

        fun setServerPriKey(serverPriKey: PrivateKey?): Builder {
            this.serverPriKey = serverPriKey
            return this
        }

        fun setServerPubKey(serverPubKey: PublicKey?): Builder {
            this.serverPubKey = serverPubKey
            return this
        }

        fun setProxyLoopGroup(proxyLoopGroup: EventLoopGroup?): Builder {
            this.proxyLoopGroup = proxyLoopGroup
            return this
        }

        fun setHandleSsl(handleSsl: Boolean): Builder {
            this.handleSsl = handleSsl
            return this
        }

        fun setBossGroupThreads(bossGroupThreads: Int): Builder {
            this.bossGroupThreads = bossGroupThreads
            return this
        }

        fun setWorkerGroupThreads(workerGroupThreads: Int): Builder {
            this.workerGroupThreads = workerGroupThreads
            return this
        }

        fun setProxyGroupThreads(proxyGroupThreads: Int): Builder {
            this.proxyGroupThreads = proxyGroupThreads
            return this
        }

        fun setHttpProxyAcceptHandler(httpProxyAcceptHandler: HttpProxyAcceptHandler?): Builder {
            this.httpProxyAcceptHandler = httpProxyAcceptHandler
            return this
        }

        fun setAuthenticationProvider(authenticationProvider: HttpProxyAuthenticationProvider<*>?): Builder {
            this.authenticationProvider = authenticationProvider
            return this
        }

        fun setMaxInitialLineLength(maxInitialLineLength: Int): Builder {
            this.maxInitialLineLength = maxInitialLineLength
            return this
        }

        fun setMaxHeaderSize(maxHeaderSize: Int): Builder {
            this.maxHeaderSize = maxHeaderSize
            return this
        }

        fun setMaxChunkSize(maxChunkSize: Int): Builder {
            this.maxChunkSize = maxChunkSize
            return this
        }

        fun build(): HttpProxyServerConfig {
            return HttpProxyServerConfig(this)
        }
    }
}
