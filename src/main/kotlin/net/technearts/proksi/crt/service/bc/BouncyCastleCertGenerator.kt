package net.technearts.proksi.crt.service.bc

import net.technearts.proksi.crt.spi.CertGenerator
import net.technearts.proksi.crt.spi.CertGeneratorInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.CertIOException
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Usa o gerador de certificados do BC Suite.
 *
 */
@CertGeneratorInfo(name = "BouncyCastle")
class BouncyCastleCertGenerator : CertGenerator {
    @Throws(Exception::class)
    override fun generateServerCert(
        issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
        caNotAfter: Date?, serverPubKey: PublicKey?,
        vararg hosts: String?
    ): X509Certificate {
        /* String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;*/
        // Gere dinamicamente o certificado do servidor de destino de acordo com o assunto do certificado CA issuer和subject
        val subject = Stream.of(*issuer!!.split(", ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()).map { item: String ->
            val arr = item.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if ("CN" == arr[0]) {
                return@map "CN=" + hosts[0]
            } else {
                return@map item
            }
        }.collect(Collectors.joining(", "))

        //doc from https://www.cryptoworkshop.com/guide/
        val jv3Builder = JcaX509v3CertificateBuilder(
            X500Name(issuer),  // problema # 3 Corrija o problema de certificado inseguro no ElementaryOS (o certificado solicitará inseguro quando o serialNumber for 1), evite conflitos de serialNumber e use timestamp + geração de número aleatório de 4 dígitos
            BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
            caNotBefore,
            caNotAfter,
            X500Name(subject),
            serverPubKey
        )
        // O nome de domínio suportado pelo certificado de extensão SAN, caso contrário, o navegador avisa que o certificado não é seguro
        val generalNames = arrayOfNulls<GeneralName>(hosts.size)
        for (i in hosts.indices) {
            generalNames[i] = GeneralName(GeneralName.dNSName, hosts[i])
        }
        val subjectAltName = GeneralNames(generalNames)
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName)
        //SHA256 usando o navegador SHA1 pode solicitar que o certificado não seja seguro
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey)
        return JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer))
    }

    @Throws(CertIOException::class, OperatorCreationException::class, CertificateException::class)
    override fun generateCaCert(
        subject: String?,
        caNotBefore: Date?,
        caNotAfter: Date?,
        keyPair: KeyPair?
    ): X509Certificate {
        val jv3Builder = JcaX509v3CertificateBuilder(
            X500Name(subject),
            BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
            caNotBefore,
            caNotAfter,
            X500Name(subject),
            keyPair!!.public
        )
        jv3Builder.addExtension(Extension.basicConstraints, true, BasicConstraints(0))
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer))
    }

    companion object {
        init {
            // Registrar a biblioteca de criptografia BouncyCastleProvider
            Security.addProvider(BouncyCastleProvider())
        }

        private val keyFactory: KeyFactory? = null
    }
}
