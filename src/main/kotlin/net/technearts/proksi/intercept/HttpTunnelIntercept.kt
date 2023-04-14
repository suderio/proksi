package net.technearts.proksi.intercept

import net.technearts.proksi.util.ProtoUtil

/**
 * @Author LiWei
 * @Description 用于拦截隧道请求，在代理服务器与目标服务器连接前
 * @Date 2019/11/4 9:57
 */
interface HttpTunnelIntercept {
    fun handle(requestProto: ProtoUtil.RequestProto?)
}
