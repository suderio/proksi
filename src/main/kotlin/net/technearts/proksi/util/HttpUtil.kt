package net.technearts.proksi.util

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.AsciiString

object HttpUtil {
    /**
     * Check if the url matches
     */
    fun checkUrl(httpRequest: HttpRequest, regex: String?): Boolean {
        val host = httpRequest.headers()[HttpHeaderNames.HOST]
        if (host != null && regex != null) {
            val url: String = if (httpRequest.uri().indexOf("/") == 0) {
                if (httpRequest.uri().length > 1) {
                    host + httpRequest.uri()
                } else {
                    host
                }
            } else {
                httpRequest.uri()
            }
            return url.matches(regex.toRegex())
        }
        return false
    }

    /**
     * Check if the value in the header is expected
     *
     * @param httpHeaders
     * @param name
     * @param regex
     * @return
     */
    fun checkHeader(httpHeaders: HttpHeaders, name: AsciiString?, regex: String): Boolean {
        val s = httpHeaders[name]
        return s != null && s.matches(regex.toRegex())
    }

    /**
     * Detect whether a web resource is requested
     */
    fun isHtml(httpRequest: HttpRequest, httpResponse: HttpResponse): Boolean {
        val accept = httpRequest.headers()[HttpHeaderNames.ACCEPT]
        val contentType = httpResponse.headers()[HttpHeaderNames.CONTENT_TYPE]
        return httpResponse.status().code() == 200 && accept != null && accept
            .matches("^.*text/html.*$".toRegex()) && contentType != null && contentType
            .matches("^text/html.*$".toRegex())
    }
}
