package net.technearts.proksi

import net.technearts.proksi.proxy.ProxyConfig
import net.technearts.proksi.proxy.ProxyType
import net.technearts.proksi.server.HttpProxyServer
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(name = "greeting", mixinStandardHelpOptions = true)
class GreetingCommand : Runnable {

    @Parameters(paramLabel = "<port>", defaultValue = "9999", description = ["Port"])
    var port: Int = 9999
    override fun run() {
        println("start proxy server at $port")
        HttpProxyServer()
            .proxyConfig(ProxyConfig(ProxyType.HTTP, "127.0.0.1", 3128))
            .start(port)
    }

}