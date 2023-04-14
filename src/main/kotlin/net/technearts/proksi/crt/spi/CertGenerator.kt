package net.technearts.proksi.crt.spi

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.*

/**
 * 证书生成器接口.
 *
 *
 *  该接口用于在无法使用本库内置的 BC 加密套件时, 可自行实现具体加密细节以绕过 BC 加密套件.
 *
 *  实现后, 请注意对实现添加 [CertGeneratorInfo] 注解, 并按照 SPI 机制规范注册实现.
 *
 * @author LamGC
 */
interface CertGenerator {
    /**
     * 生成服务端自签名证书.
     * @param issuer 元数据(X509 Names)
     * @param caPriKey 用于进行签名的 CA 私钥.
     * @param caNotBefore 证书生效时间, 在这个时间之前证书也是失效的.
     * @param caNotAfter 证书失效时间, 过了这个时间后证书即失效.
     * @param serverPubKey 服务端证书公钥.
     * @param hosts 证书所属域名.
     * @return 返回指定域名所属的服务端 X509 证书.
     * @throws Exception 当发生任意异常时, 异常将直接抛出至调用方.
     */
    @Throws(Exception::class)
    fun generateServerCert(
        issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
        caNotAfter: Date?, serverPubKey: PublicKey?,
        vararg hosts: String?
    ): X509Certificate

    /**
     * 生成 CA 证书(自签名).
     * @param subject 元数据(X509 Names)
     * @param caNotBefore 证书生效时间, 在这个时间之前证书也是失效的.
     * @param caNotAfter 证书失效时间, 过了这个时间后证书即失效.
     * @param keyPair RSA 密钥对.
     * @return 返回自签名 CA 证书.
     * @throws Exception 当发生任意异常时, 异常将直接抛出至调用方.
     */
    @Throws(Exception::class)
    fun generateCaCert(subject: String?, caNotBefore: Date?, caNotAfter: Date?, keyPair: KeyPair?): X509Certificate
}
