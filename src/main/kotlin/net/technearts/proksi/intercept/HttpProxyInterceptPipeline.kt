package net.technearts.proksi.intercept

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import net.technearts.proksi.proxy.ProxyConfig
import net.technearts.proksi.util.ProtoUtil
import java.util.*

class HttpProxyInterceptPipeline(defaultIntercept: HttpProxyIntercept) : Iterable<HttpProxyIntercept?> {
    private val intercepts: MutableList<HttpProxyIntercept>
    private var posBeforeConnect = 0
    private var posBeforeHead = 0
    private var posBeforeContent = 0
    private var posAfterHead = 0
    private var posAfterContent = 0
    var requestProto: ProtoUtil.RequestProto? = null
    var httpRequest: HttpRequest? = null
        private set
    var httpResponse: HttpResponse? = null
        private set
    var proxyConfig: ProxyConfig? = null

    init {
        intercepts = LinkedList()
        intercepts.add(defaultIntercept)
    }

    fun addLast(intercept: HttpProxyIntercept) {
        intercepts.add(intercepts.size - 1, intercept)
    }

    fun addFirst(intercept: HttpProxyIntercept) {
        intercepts.add(0, intercept)
    }

    operator fun get(index: Int): HttpProxyIntercept {
        return intercepts[index]
    }

    @Throws(Exception::class)
    fun beforeConnect(clientChannel: Channel?) {
        if (posBeforeConnect < intercepts.size) {
            val intercept = intercepts[posBeforeConnect++]
            intercept.beforeConnect(clientChannel, this)
        }
        posBeforeConnect = 0
    }

    @Throws(Exception::class)
    fun beforeRequest(clientChannel: Channel, httpRequest: HttpRequest) {
        this.httpRequest = httpRequest
        if (posBeforeHead < intercepts.size) {
            val intercept = intercepts[posBeforeHead++]
            intercept.beforeRequest(clientChannel, this.httpRequest!!, this)
        }
        posBeforeHead = 0
    }

    @Throws(Exception::class)
    fun beforeRequest(clientChannel: Channel, httpContent: HttpContent) {
        if (posBeforeContent < intercepts.size) {
            val intercept = intercepts[posBeforeContent++]
            intercept.beforeRequest(clientChannel, httpContent, this)
        }
        posBeforeContent = 0
    }

    @Throws(Exception::class)
    fun afterResponse(clientChannel: Channel?, proxyChannel: Channel?, httpResponse: HttpResponse?) {
        this.httpResponse = httpResponse
        if (posAfterHead < intercepts.size) {
            val intercept = intercepts[posAfterHead++]
            intercept.afterResponse(clientChannel, proxyChannel, this.httpResponse, this)
        }
        posAfterHead = 0
    }

    @Throws(Exception::class)
    fun afterResponse(clientChannel: Channel?, proxyChannel: Channel?, httpContent: HttpContent?) {
        if (posAfterContent < intercepts.size) {
            val intercept = intercepts[posAfterContent++]
            intercept.afterResponse(clientChannel, proxyChannel, httpContent, this)
        }
        posAfterContent = 0
    }

    fun posBeforeHead(): Int {
        return posBeforeHead
    }

    fun posBeforeContent(): Int {
        return posBeforeContent
    }

    fun posAfterHead(): Int {
        return posAfterHead
    }

    fun posAfterContent(): Int {
        return posAfterContent
    }

    fun posBeforeHead(pos: Int) {
        posBeforeHead = pos
    }

    fun posBeforeContent(pos: Int) {
        posBeforeContent = pos
    }

    fun posAfterHead(pos: Int) {
        posAfterHead = pos
    }

    fun posAfterContent(pos: Int) {
        posAfterContent = pos
    }

    fun resetBeforeHead() {
        posBeforeHead(0)
    }

    fun resetBeforeContent() {
        posBeforeContent(0)
    }

    fun resetAfterHead() {
        posAfterHead(0)
    }

    fun resetAfterContent() {
        posAfterContent(0)
    }

    override fun iterator(): MutableIterator<HttpProxyIntercept> {
        return intercepts.iterator()
    }
}
