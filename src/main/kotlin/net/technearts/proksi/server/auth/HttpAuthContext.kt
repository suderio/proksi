package net.technearts.proksi.server.auth

import io.netty.channel.Channel
import net.technearts.proksi.server.auth.model.HttpToken
import net.technearts.proksi.server.context.HttpContext.get
import net.technearts.proksi.server.context.HttpContext.set

/**
 * @Author LiWei
 * @Description
 * @Date 2021/8/3 11:37
 */
object HttpAuthContext {
    private const val AUTH_KEY = "http_auth"
    fun getToken(clientChanel: Channel?): HttpToken {
        return get(
            clientChanel!!, AUTH_KEY
        )
    }

    fun setToken(clientChanel: Channel?, httpToken: HttpToken) {
        set(
            clientChanel!!, AUTH_KEY, httpToken
        )
    }
}
