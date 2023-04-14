package net.technearts.proksi.crt

import net.technearts.proksi.crt.CertUtilsLoader.currentSelectionGenerator
import net.technearts.proksi.crt.CertUtilsLoader.generateCaCert
import net.technearts.proksi.crt.CertUtilsLoader.generateServerCert
import net.technearts.proksi.crt.CertUtilsLoader.setSelectionGenerator
import java.io.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.IntStream

object CertUtil {
    private var keyFactory: KeyFactory? = null
        get() {
            if (field == null) {
                field = try {
                    KeyFactory.getInstance("RSA")
                } catch (e: NoSuchAlgorithmException) {
                    // Unexpected anomalies
                    throw IllegalStateException(e)
                }
            }
            return field
        }

    /**
     * 生成RSA公私密钥对,长度为2048
     */
    @Throws(Exception::class)
    fun genKeyPair(): KeyPair {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048, SecureRandom())
        return keyPairGen.genKeyPair()
    }

    /**
     * 从文件加载RSA私钥
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun loadPriKey(bts: ByteArray?): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bts)
        return keyFactory!!.generatePrivate(privateKeySpec)
    }

    /**
     * 从文件加载RSA私钥
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(Exception::class)
    fun loadPriKey(path: String): PrivateKey {
        return loadPriKey(Files.readAllBytes(Paths.get(path)))
    }

    /**
     * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    @Throws(Exception::class)
    fun loadPriKey(uri: URI): PrivateKey {
        return loadPriKey(Paths.get(uri).toString())
    }

    /**
     * 从文件加载RSA私钥
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(IOException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    fun loadPriKey(inputStream: InputStream): PrivateKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)
        var len: Int
        while (inputStream.read(bts).also { len = it } != -1) {
            outputStream.write(bts, 0, len)
        }
        inputStream.close()
        outputStream.close()
        return loadPriKey(outputStream.toByteArray())
    }

    /**
     * 从文件加载RSA公钥
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(bts: ByteArray?): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(bts)
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * 从文件加载RSA公钥
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(path: String): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Files.readAllBytes(Paths.get(path)))
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * 从文件加载RSA公钥
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(uri: URI): PublicKey {
        return loadPubKey(Paths.get(uri).toString())
    }

    /**
     * 从文件加载RSA公钥
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(inputStream: InputStream): PublicKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)
        var len: Int
        while (inputStream.read(bts).also { len = it } != -1) {
            outputStream.write(bts, 0, len)
        }
        inputStream.close()
        outputStream.close()
        return loadPubKey(outputStream.toByteArray())
    }

    /**
     * 从文件加载证书
     */
    @Throws(CertificateException::class, IOException::class)
    fun loadCert(inputStream: InputStream): X509Certificate {
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            cf.generateCertificate(inputStream) as X509Certificate
        } finally {
            inputStream.close()
        }
    }

    /**
     * 从文件加载证书
     */
    @Throws(Exception::class)
    fun loadCert(path: String): X509Certificate {
        return loadCert(FileInputStream(path))
    }

    /**
     * 从文件加载证书
     */
    @Throws(Exception::class)
    fun loadCert(uri: URI): X509Certificate {
        return loadCert(Paths.get(uri).toString())
    }

    /**
     * 读取ssl证书使用者信息
     */
    @Throws(Exception::class)
    fun getSubject(inputStream: InputStream): String {
        val certificate = loadCert(inputStream)
        //读出来顺序是反的需要反转下
        val tempList =
            listOf(*certificate.issuerX500Principal.toString().split(", ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
            .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * 读取ssl证书使用者信息
     */
    @Throws(Exception::class)
    fun getSubject(certificate: X509Certificate): String {
        //读出来顺序是反的需要反转下
        val tempList =
            Arrays.asList(*certificate.issuerX500Principal.toString().split(", ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
            .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * 动态生成服务器证书,并进行CA签授
     *
     * @param issuer 颁发机构
     */
    @JvmStatic
    @Throws(Exception::class)
    fun genCert(
        issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
        caNotAfter: Date?, serverPubKey: PublicKey?,
        vararg hosts: String?
    ): X509Certificate {
        return generateServerCert(issuer, caPriKey, caNotBefore, caNotAfter, serverPubKey, *hosts)
    }

    /**
     * 生成CA服务器证书
     */
    @Throws(Exception::class)
    fun genCACert(
        subject: String?, caNotBefore: Date?, caNotAfter: Date?,
        keyPair: KeyPair?
    ): X509Certificate {
        return generateCaCert(subject, caNotBefore, caNotAfter, keyPair)
    }

    var certGenerator: String?
        /**
         * 获取当前所选择的生成器名称.
         * @return 返回指定要使用的生成器名称.
         */
        get() = currentSelectionGenerator
        /**
         * 设置所使用的生成器名称.
         * @param generatorName 欲使用的生成器所属名称, 如果为 null 则恢复默认生成器.
         * @throws NoSuchElementException 如果指定名称不存在所属生成器则抛出该异常.
         */
        set(generatorName) {
            setSelectionGenerator(
                generatorName ?: CertUtilsLoader.DEFAULT_GENERATOR_NAME
            )
        }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        //生成ca证书和私钥
        val keyPair = genKeyPair()
        val caCertFile = File("./ca.crt")
        if (caCertFile.exists()) {
            caCertFile.delete()
        }
        Files.write(
            Paths.get(caCertFile.toURI()),
            genCACert(
                "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=Proxyee",
                Date(),
                Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
                keyPair
            )
                .encoded
        )
        val caPriKeyFile = File("./ca_private.der")
        if (caPriKeyFile.exists()) {
            caPriKeyFile.delete()
        }
        Files.write(
            caPriKeyFile.toPath(),
            PKCS8EncodedKeySpec(keyPair.private.encoded).encoded
        )
    }
}
