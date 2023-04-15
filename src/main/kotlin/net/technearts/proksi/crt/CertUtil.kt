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
     * Gere um par de chaves públicas-privadas RSA com um comprimento de 2048
     */
    @Throws(Exception::class)
    fun genKeyPair(): KeyPair {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048, SecureRandom())
        return keyPairGen.genKeyPair()
    }

    /**
     * Carregar chave privada RSA do arquivo
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun loadPriKey(bts: ByteArray?): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bts)
        return keyFactory!!.generatePrivate(privateKeySpec)
    }

    /**
     * Carregar chave privada RSA do arquivo
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(Exception::class)
    fun loadPriKey(path: String): PrivateKey {
        return loadPriKey(Files.readAllBytes(Paths.get(path)))
    }

    /**
     * Carregar chave privada RSA do arquivo
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der
     */
    @Throws(Exception::class)
    fun loadPriKey(uri: URI): PrivateKey {
        return loadPriKey(Paths.get(uri).toString())
    }

    /**
     * Carregar chave privada RSA do arquivo
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
     * Carregar chave pública RSA do arquivo
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(bts: ByteArray?): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(bts)
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * Carregar chave pública RSA do arquivo
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(path: String): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Files.readAllBytes(Paths.get(path)))
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * Carregar chave pública RSA do arquivo
     * openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(Exception::class)
    fun loadPubKey(uri: URI): PublicKey {
        return loadPubKey(Paths.get(uri).toString())
    }

    /**
     * Carregar chave pública RSA do arquivo
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
     * Carregar certificado do arquivo
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
     * Carregar certificado do arquivo
     */
    @Throws(Exception::class)
    fun loadCert(path: String): X509Certificate {
        return loadCert(FileInputStream(path))
    }

    /**
     * Carregar certificado do arquivo
     */
    @Throws(Exception::class)
    fun loadCert(uri: URI): X509Certificate {
        return loadCert(Paths.get(uri).toString())
    }

    /**
     * Leia as informações do usuário do certificado SSL
     */
    @Throws(Exception::class)
    fun getSubject(inputStream: InputStream): String {
        val certificate = loadCert(inputStream)
        //A ordem de leitura é invertida e precisa ser invertida
        val tempList =
            listOf(*certificate.issuerX500Principal.toString().split(", ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
            .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * Leia as informações do usuário do certificado SSL
     */
    @Throws(Exception::class)
    fun getSubject(certificate: X509Certificate): String {
        //A ordem de leitura é invertida e precisa ser invertida
        val tempList =
            Arrays.asList(*certificate.issuerX500Principal.toString().split(", ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
            .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * Gere certificados de servidor dinamicamente e execute a assinatura de CA
     *
     * @param issuer Autoridade
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
     * Gerar certificado de servidor CA
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
         * Obtenha o nome do gerador atualmente selecionado.
         * @return Retorna o nome do gerador especificado a ser usado.
         */
        get() = currentSelectionGenerator
        /**
         * Defina o nome do gerador a ser usado.
         * @param generatorName O nome do gerador a ser usado, se for nulo, restaura o gerador padrão.
         * @throws NoSuchElementException Lançado se o gerador com o nome especificado não existir.
         */
        set(generatorName) {
            setSelectionGenerator(
                generatorName ?: CertUtilsLoader.DEFAULT_GENERATOR_NAME
            )
        }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        //Gerar certificado ca e chave privada
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
