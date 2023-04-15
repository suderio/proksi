package net.technearts.proksi.crt.spi

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.*

/**
 * Interface do gerador de certificados.
 *
 *
 * Esta interface é usada para implementar detalhes de criptografia específicos para ignorar o conjunto de cifras BC quando o conjunto de cifras BC integrado desta biblioteca não pode ser usado.
 *
 * Após a implementação, preste atenção para adicionar a anotação [CertGeneratorInfo] à implementação e registre a implementação de acordo com a especificação do mecanismo SPI.
 *
 */
interface CertGenerator {
    /**
     * Gerar certificado autoassinado do servidor.
     * @param issuer do emissor (Nomes X509)
     * @param caPriKey Chave privada CA usada para assinatura.
     * @param caNotBefore A hora efetiva do certificado, o certificado também é inválido antes dessa hora.
     * @param caNotAfter o tempo de expiração do certificado, o certificado será inválido após esse tempo.
     * @param serverPubKey chave pública do certificado do servidor.
     * @param hosts O nome de domínio do certificado.
     * @return Retorna o certificado X509 do servidor ao qual pertence o nome de domínio especificado.
     * @throws Exception Quando ocorrer qualquer exceção, a exceção será lançada diretamente para o chamador.
     */
    @Throws(Exception::class)
    fun generateServerCert(
        issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
        caNotAfter: Date?, serverPubKey: PublicKey?,
        vararg hosts: String?
    ): X509Certificate

    /**
     * Gerar certificado CA (auto-assinado).
     * @param subject do assunto (Nomes X509)
     * @param caNotBefore A hora efetiva do certificado, o certificado também é inválido antes dessa hora.
     * @param caNotAfter o tempo de expiração do certificado, o certificado será inválido após esse tempo.
     * @param keyPair Par de chaves RSA.
     * @return Retorna o certificado CA autoassinado.
     * @throws Exception Quando ocorrer qualquer exceção, a exceção será lançada diretamente para o chamador.
     */
    @Throws(Exception::class)
    fun generateCaCert(subject: String?, caNotBefore: Date?, caNotAfter: Date?, keyPair: KeyPair?): X509Certificate
}
