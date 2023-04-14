package net.technearts.proksi.server.auth.model

/**
 * @Author LiWei
 * @Description
 * @Date 2021/8/3 11:26
 */
class BasicHttpToken : HttpToken {
    var usr: String? = null
    var pwd: String? = null

    constructor()
    constructor(usr: String?, pwd: String?) {
        this.usr = usr
        this.pwd = pwd
    }

    override fun toString(): String {
        return "HttpBasicToken{" +
                "usr='" + usr + '\'' +
                ", pwd='" + pwd + '\'' +
                '}'
    }
}
