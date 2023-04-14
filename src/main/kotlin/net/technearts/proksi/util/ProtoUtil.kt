package net.technearts.proksi.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import java.io.Serializable
import java.net.MalformedURLException
import java.net.URL
import java.util.*

object ProtoUtil {
    /*
    代理服务器需要处理两种握手类型，一种是非CONNECT的http报文代理，另外一种是CONNECT的TCP报文原始转发
    示例：
        GET http://www.google.com/ HTTP/1.1
        CONNECT www.google.com:443 HTTP/1.1
        CONNECT echo.websocket.org:443 HTTP/1.1
        CONNECT echo.websocket.org:80 HTTP/1.1
    当客户端请求协议为TLS(https、wss)、WebSocket(ws)的时候，都会发起CONNECT请求进行原始转发，
    所以在握手的时候是无法区分客户端原始请求是否为TLS。
     */
    fun getRequestProto(httpRequest: HttpRequest): RequestProto? {
        val requestProto = RequestProto()
        var uri = httpRequest.uri().lowercase(Locale.getDefault())
        if (!uri.startsWith("http://")) {
            uri = "http://$uri"
        }
        val url: URL
        url = try {
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
         * 请求是否来源于http代理，用于区分是通过代理服务访问的还是直接通过http访问的代理服务器
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
