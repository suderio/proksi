package net.technearts.proksi.crt

import io.netty.util.internal.logging.InternalLoggerFactory
import net.technearts.proksi.crt.service.bc.BouncyCastleCertGenerator
import net.technearts.proksi.crt.spi.CertGenerator
import net.technearts.proksi.crt.spi.CertGeneratorInfo
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Classe interna de CertUtil.
 *
 * Por meio do [CertGenerator], o Proxyee não depende mais do pacote de criptografia BC e pode fornecer as operações relevantes do pacote ao Proxyee de acordo com as necessidades.
 *
 */
object CertUtilsLoader {
    private val log = InternalLoggerFactory.getInstance(CertUtilsLoader::class.java)
    const val DEFAULT_GENERATOR_NAME = "BouncyCastle"
    private val generatorMap: MutableMap<String, CertGenerator> = Hashtable()
    private val selectionGenerator = AtomicReference(DEFAULT_GENERATOR_NAME)

    init {
        try {
            loadGenerator()
        } catch (e: Exception) {
            // Intercepte exceções não consideradas para evitar falhas de carregamento de classes e, em seguida, programe o Boom!
            // Para garantir a operação normal mesmo se o carregamento falhar, carregue a implementação de criptografia padrão BC nesta exceção (manter a robustez)
            log.error("An uncaught exception was thrown while loading the certificate generator", e)
            generatorMap[DEFAULT_GENERATOR_NAME] = BouncyCastleCertGenerator()
        }
    }

    /**
     * Carregue e verifique a implementação do gerador de certificados por meio do mecanismo SPI.
     */
    private fun loadGenerator() {
        val serviceLoader = ServiceLoader.load(
            CertGenerator::class.java
        )
        for (generator in serviceLoader) {
            val generatorClass: Class<out CertGenerator> = generator.javaClass
            if (!generatorClass.isAnnotationPresent(CertGeneratorInfo::class.java)) {
                log.warn(
                    "CertGeneratorInfo annotation not found for implementation class {}",
                    generatorClass.name
                )
                continue
            }
            val info = generatorClass.getAnnotation(
                CertGeneratorInfo::class.java
            )
            val generatorName = info.name.trim { it <= ' ' }
            if (generatorName.isEmpty()) {
                log.warn("Implementation class {} name is empty", generatorClass.name)
                continue
            } else if (generatorMap.containsKey(generatorName)) {
                val currentGenerator = generatorMap[generatorName]
                log.warn(
                    "A loaded implementation already exists for name {} (current implementation: {}), " +
                            "skipping implementation {}}",
                    generatorName,
                    currentGenerator!!.javaClass.name,
                    generatorClass.name
                )
                continue
            }
            generatorMap[generatorName] = generator
            log.debug(
                "Generator implementation loaded (Name: {}, implementation class: {})",
                generatorName,
                generatorClass.name
            )
        }
    }

    /**
     * Defina o nome do gerador a ser usado.
     * @param generatorName O nome do gerador a ser usado.
     * @throws NoSuchElementException Lançado se o gerador com o nome especificado não existir.
     */
    @JvmStatic
    @Throws(NoSuchElementException::class)
    fun setSelectionGenerator(generatorName: String) {
        if (!generatorMap.containsKey(generatorName.trim { it <= ' ' })) {
            throw NoSuchElementException("The specified generator was not found: $generatorName")
        }
        selectionGenerator.set(generatorName.trim { it <= ' ' })
    }

    /**
     * Verifique se o gerador existe e pegue-o.
     * @param name do gerador de nomes.
     * @return Se presente, retorna o gerador com o nome especificado.
     * @throws NullPointerException Se o nome for nulo, lança NPE.
     * @throws NoSuchElementException Lançada se o gerador especificado por name não existir.
     */
    private fun checkGenerateExistAndGet(name: String): CertGenerator {
        if (!generatorMap.containsKey(name)) {
            // u1s1, Unchecked Exception é realmente uma coisa boa.
            throw NoSuchElementException(
                "The certificate generator with the specified name was not found: $name"
            )
        }
        return generatorMap[name]!!
    }

    @JvmStatic
    val currentSelectionGenerator: String
        /**
         * Obtenha o nome do gerador atualmente selecionado.
         * @return Retorna o nome do gerador especificado a ser usado.
         */
        get() = selectionGenerator.get()

    /**
     * Gerar certificado autoassinado do servidor.
     * @param issuer do emissor (Nomes X509)
     * @param caPriKey Chave privada CA usada para assinatura.
     * @param caNotBefore A hora efetiva do certificado, o certificado também é inválido antes dessa hora.
     * @param caNotAfter o tempo de expiração do certificado, o certificado será inválido após esse tempo.
     * @param serverPubKey chave pública do certificado do servidor.
     * @param hosts O nome de domínio do certificado.
     * @return Retorna o certificado X509 do servidor ao qual pertence o nome de domínio especificado.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun generateServerCert(
        issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
        caNotAfter: Date?, serverPubKey: PublicKey?, vararg hosts: String?
    ): X509Certificate {
        return checkGenerateExistAndGet(selectionGenerator.get())
            .generateServerCert(
                issuer, caPriKey, caNotBefore,
                caNotAfter, serverPubKey, *hosts
            )
    }

    /**
     * Gerar certificado CA (auto-assinado).
     * @param subject do assunto (Nomes X509)
     * @param caNotBefore A hora efetiva do certificado, o certificado também é inválido antes dessa hora.
     * @param caNotAfter o tempo de expiração do certificado, o certificado será inválido após esse tempo.
     * @param keyPair Par de chaves RSA.
     * @return Retorna o certificado CA autoassinado.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun generateCaCert(
        subject: String?, caNotBefore: Date?,
        caNotAfter: Date?, keyPair: KeyPair?
    ): X509Certificate {
        return checkGenerateExistAndGet(selectionGenerator.get())
            .generateCaCert(
                subject, caNotBefore, caNotAfter, keyPair
            )
    }
}
