package net.technearts.proksi.crt

import net.technearts.proksi.crt.CertUtil.genCert
import net.technearts.proksi.server.HttpProxyServerConfig
import java.security.cert.X509Certificate
import java.util.*

object CertPool {
    private val certCache: MutableMap<Int, MutableMap<String, X509Certificate>> = WeakHashMap()
    @Throws(Exception::class)
    fun getCert(port: Int, host: String?, serverConfig: HttpProxyServerConfig): X509Certificate? {
        var cert: X509Certificate? = null
        if (host != null) {
            var portCertCache = certCache[port]
            if (portCertCache == null) {
                portCertCache = HashMap()
                certCache[port] = portCertCache
            }
            val key = host.trim { it <= ' ' }.lowercase(Locale.getDefault())
            if (portCertCache.containsKey(key)) {
                return portCertCache[key]
            } else {
                cert = genCert(
                    serverConfig.issuer, serverConfig.caPriKey,
                    serverConfig.caNotBefore, serverConfig.caNotAfter,
                    serverConfig.serverPubKey, key
                )
                portCertCache[key] = cert
            }
        }
        return cert
    }

    fun clear() {
        certCache.clear()
    }
}
