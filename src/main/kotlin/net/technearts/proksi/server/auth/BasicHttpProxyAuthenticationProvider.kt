package net.technearts.proksi.server.auth

import net.technearts.proksi.server.auth.model.BasicHttpToken
import java.util.*

abstract class BasicHttpProxyAuthenticationProvider : HttpProxyAuthenticationProvider<BasicHttpToken?> {
    override fun authType(): String? {
        return AUTH_TYPE_BASIC
    }

    override fun authRealm(): String? {
        return AUTH_REALM_BASIC
    }

    protected abstract fun authenticate(usr: String?, pwd: String?): BasicHttpToken?
    override fun authenticate(authorization: String?): BasicHttpToken? {
        var usr = ""
        var pwd = ""
        if (!authorization.isNullOrEmpty()) {
            val token = authorization.substring(AUTH_TYPE_BASIC.length + 1)
            val decode = String(Base64.getDecoder().decode(token))
            val arr = decode.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (arr.isNotEmpty()) {
                usr = arr[0]
            }
            if (arr.size >= 2) {
                pwd = arr[1]
            }
        }
        return authenticate(usr, pwd)
    }

    companion object {
        const val AUTH_TYPE_BASIC = "Basic"
        const val AUTH_REALM_BASIC = "Access to the staging site"
    }
}
