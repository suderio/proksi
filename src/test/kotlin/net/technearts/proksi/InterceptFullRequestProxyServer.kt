package net.technearts.proksi

import io.netty.handler.codec.http.*
import net.technearts.proksi.intercept.HttpProxyInterceptInitializer
import net.technearts.proksi.intercept.HttpProxyInterceptPipeline
import net.technearts.proksi.intercept.common.CertDownIntercept
import net.technearts.proksi.intercept.common.FullRequestIntercept
import net.technearts.proksi.server.HttpProxyServer
import net.technearts.proksi.server.HttpProxyServerConfig
import net.technearts.proksi.util.HttpUtil.checkHeader
import java.nio.charset.Charset

object InterceptFullRequestProxyServer {
    /*
      curl -k -x 127.0.0.1:9999 \
      -X POST \
      http://www.baidu.com \
      -H 'Content-Type: application/json' \
      -d '{"name":"admin","pwd":"123456"}'

      echo '{"name":"admin","pwd":"123456"}' | gzip | \
          curl -x 127.0.0.1:9999 \
          http://www.baidu.com \
          -H "Content-Encoding: gzip" \
          -H "Content-Type: application/json" \
          --data-binary @-
     */
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val config = HttpProxyServerConfig()
        config.isHandleSsl = true
        HttpProxyServer()
            .serverConfig(config)
            .proxyInterceptInitializer(object : HttpProxyInterceptInitializer() {
                override fun init(pipeline: HttpProxyInterceptPipeline?) {
                    pipeline!!.addLast(CertDownIntercept())
                    pipeline.addLast(object : FullRequestIntercept() {
                        override fun match(httpRequest: HttpRequest?, pipeline: HttpProxyInterceptPipeline?): Boolean {
                            //如果是json报文
                            return if (checkHeader(
                                    httpRequest!!.headers(),
                                    HttpHeaderNames.CONTENT_TYPE,
                                    "^(?i)application/json.*$"
                                )
                            ) {
                                true
                            } else false
                        }

                        override fun handleRequest(
                            httpRequest: FullHttpRequest?,
                            pipeline: HttpProxyInterceptPipeline?
                        ) {
                            val content = httpRequest!!.content()
                            //打印请求信息
                            println(httpRequest.toString())
                            println(content.toString(Charset.defaultCharset()))
                            //修改请求体
                            val body = "{\"name\":\"intercept\",\"pwd\":\"123456\"}"
                            content.clear()
                            content.writeBytes(body.toByteArray())
                        }
                    })
                }
            })
            .start(9999)
    }
}
