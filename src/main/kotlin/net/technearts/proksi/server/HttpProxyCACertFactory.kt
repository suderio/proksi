package net.technearts.proksi.server

import java.security.PrivateKey
import java.security.cert.X509Certificate

interface HttpProxyCACertFactory {
    @get:Throws(Exception::class)
    val cACert: X509Certificate?

    @get:Throws(Exception::class)
    val cAPriKey: PrivateKey?
}
