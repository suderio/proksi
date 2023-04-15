package net.technearts.proksi.intercept

import net.technearts.proksi.util.ProtoUtil

/**
 * @Description is used to intercept tunnel requests, before the proxy server connects to the target server
 */
interface HttpTunnelIntercept {
    fun handle(requestProto: ProtoUtil.RequestProto?)
}
