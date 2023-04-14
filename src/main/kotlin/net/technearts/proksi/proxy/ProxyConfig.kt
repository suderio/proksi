package net.technearts.proksi.proxy

import java.io.Serializable

class ProxyConfig : Serializable {
    var proxyType: ProxyType? = null
    var host: String? = null
    var port = 0
    var user: String? = null
    var pwd: String? = null

    constructor()
    constructor(proxyType: ProxyType?, host: String?, port: Int) {
        this.proxyType = proxyType
        this.host = host
        this.port = port
    }

    constructor(proxyType: ProxyType?, host: String?, port: Int, user: String?, pwd: String?) {
        this.proxyType = proxyType
        this.host = host
        this.port = port
        this.user = user
        this.pwd = pwd
    }

    override fun toString(): String {
        return "ProxyConfig{" +
                "proxyType=" + proxyType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", user='" + user + '\'' +
                ", pwd='" + pwd + '\'' +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val config = other as ProxyConfig
        if (port != config.port) {
            return false
        }
        if (proxyType !== config.proxyType) {
            return false
        }
        if (if (host != null) host != config.host else config.host != null) {
            return false
        }
        if (if (user != null) user != config.user else config.user != null) {
            return false
        }
        return if (pwd != null) pwd == config.pwd else config.pwd == null
    }

    override fun hashCode(): Int {
        var result = if (proxyType != null) proxyType.hashCode() else 0
        result = 31 * result + if (host != null) host.hashCode() else 0
        result = 31 * result + port
        result = 31 * result + if (user != null) user.hashCode() else 0
        result = 31 * result + if (pwd != null) pwd.hashCode() else 0
        return result
    }

    companion object {
        private const val serialVersionUID = 1531104384359036231L
    }
}
