package net.technearts.proksi.util

import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

object ByteUtil {
    fun findText(byteBuf: ByteBuf, str: String): Int {
        val text = str.toByteArray()
        var matchIndex = 0
        for (i in byteBuf.readerIndex() until byteBuf.readableBytes()) {
            for (j in matchIndex until text.size) {
                if (byteBuf.getByte(i) == text[j]) {
                    matchIndex = j + 1
                    if (matchIndex == text.size) {
                        return i
                    }
                } else {
                    matchIndex = 0
                }
                break
            }
        }
        return -1
    }

    @JvmOverloads
    fun insertText(byteBuf: ByteBuf, index: Int, str: String, charset: Charset? = Charset.defaultCharset()): ByteBuf {
        val begin = ByteArray(index + 1)
        val end = ByteArray(byteBuf.readableBytes() - begin.size)
        byteBuf.readBytes(begin)
        byteBuf.readBytes(end)
        byteBuf.writeBytes(begin)
        byteBuf.writeBytes(str.toByteArray(charset!!))
        byteBuf.writeBytes(end)
        return byteBuf
    }
}
