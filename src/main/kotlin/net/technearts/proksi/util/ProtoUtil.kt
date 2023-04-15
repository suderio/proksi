package net.technearts.proksi.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import java.io.Serializable
import java.net.MalformedURLException
import java.net.URL
import java.util.*

object ProtoUtil {
    /*
     The proxy server needs to handle two types of handshakes, one is non-CONNECT http packet proxy, and the other is the original forwarding of CONNECT TCP packets
     Example:
         GET http://www.google.com/ HTTP/1.1
         CONNECT www.google.com:443 HTTP/1.1
         CONNECT echo.websocket.org:443 HTTP/1.1
         CONNECT echo.websocket.org:80 HTTP/1.1
     When the client request protocol is TLS (https, wss) or WebSocket (ws), it will initiate a CONNECT request for original forwarding.
     Therefore, it is impossible to distinguish whether the original request of the client is TLS during the handshake.
      */
    fun getRequestProto(httpRequest: HttpRequest): RequestProto? {
        val requestProto = RequestProto()
        var uri = httpRequest.uri().lowercase(Locale.getDefault())
        if (!uri.startsWith("http://")) {
            uri = "http://$uri"
        }
        val url: URL = try {
            URL(uri)
        } catch (e: MalformedURLException) {
            return null
        }
        requestProto.host = url.host.ifEmpty { httpRequest.headers()[HttpHeaderNames.HOST] }
        requestProto.port = if (url.port != -1) url.port else url.defaultPort
        requestProto.proxy = httpRequest.headers().contains(HttpHeaderNames.CONNECTION)
        return requestProto
    }

    class RequestProto : Serializable {
        /**
         * Whether the request comes from an http proxy, used to distinguish whether it is accessed through a proxy
         * service or a proxy server accessed directly through http
         */
        var proxy = false
        var host: String? = null
        var port = 0
        var ssl = false

        constructor()
        constructor(host: String?, port: Int, ssl: Boolean) {
            this.host = host
            this.port = port
            this.ssl = ssl
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as RequestProto
            return port == that.port && ssl == that.ssl && host == that.host
        }

        override fun hashCode(): Int {
            return Objects.hash(host, port, ssl)
        }

        fun copy(): RequestProto {
            val requestProto = RequestProto()
            requestProto.proxy = proxy
            requestProto.host = host
            requestProto.port = port
            requestProto.ssl = ssl
            return requestProto
        }

        companion object {
            private const val serialVersionUID = -6471051659605127698L
        }
    }
}
