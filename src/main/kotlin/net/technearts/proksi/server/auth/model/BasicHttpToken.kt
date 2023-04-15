package net.technearts.proksi.server.auth.model

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
