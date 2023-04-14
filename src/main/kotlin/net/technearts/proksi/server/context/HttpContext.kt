package net.technearts.proksi.server.context

import io.netty.channel.Channel
import io.netty.util.AttributeKey

/**
 * @Author LiWei
 * @Description
 * @Date 2021/8/3 11:33
 */
object HttpContext {
    operator fun <T> get(channel: Channel, key: String?): T {
        return channel.attr(AttributeKey.valueOf<T>(key)).get()
    }

    operator fun <T> set(channel: Channel, key: String?, value: T) {
        channel.attr(AttributeKey.valueOf<T>(key)).set(value)
    }
}
