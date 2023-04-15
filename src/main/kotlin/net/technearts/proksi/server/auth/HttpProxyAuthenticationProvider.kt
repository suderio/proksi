package net.technearts.proksi.server.auth

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import net.technearts.proksi.server.auth.model.HttpToken

interface HttpProxyAuthenticationProvider<R : HttpToken?> {
    fun authType(): String?
    fun authRealm(): String?
    fun authenticate(authorization: String?): R
    fun authenticate(request: HttpRequest): R {
        return authenticate(request.headers()[HttpHeaderNames.PROXY_AUTHORIZATION])
    }

    fun matches(request: HttpRequest?): Boolean {
        return true
    }
}
