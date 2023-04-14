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
 * CertUtil 的内部类.
 *
 * 通过 [CertGenerator] 让 Proxyee 不再固定依赖 BC 加密套件, 可根据需要自行指定套件的相关操作提供给 Proxyee.
 *
 * @author LamGC
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
            // 拦截未考虑的异常以防导致类加载失败, 然后程序 Boom!
            // 为保证即使加载失败也能正常运行, 在此例外加载 BC 默认加密实现(保持鲁棒性)
            log.error("An uncaught exception was thrown while loading the certificate generator", e)
            generatorMap[DEFAULT_GENERATOR_NAME] = BouncyCastleCertGenerator()
        }
    }

    /**
     * 通过 SPI 机制加载并验证证书生成器实现.
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
     * 设置所使用的生成器名称.
     * @param generatorName 欲使用的生成器所属名称.
     * @throws NoSuchElementException 如果指定名称不存在所属生成器则抛出该异常.
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
     * 检查生成器是否存在并获取.
     * @param name 生成器名称.
     * @return 如果存在, 返回指定名称的生成器.
     * @throws NullPointerException 如果 name 为 null, 则抛出 NPE.
     * @throws NoSuchElementException 如果 name 指定的生成器不存在, 则抛出该异常.
     */
    private fun checkGenerateExistAndGet(name: String): CertGenerator {
        if (!generatorMap.containsKey(name)) {
            // u1s1, Unchecked Exception 确实是个好东西.
            throw NoSuchElementException(
                "The certificate generator with the specified name was not found: $name"
            )
        }
        return generatorMap[name]!!
    }

    @JvmStatic
    val currentSelectionGenerator: String
        /**
         * 获取当前所选择的生成器名称.
         * @return 返回指定要使用的生成器名称.
         */
        get() = selectionGenerator.get()

    /**
     * 生成服务端自签名证书.
     * @param issuer 元数据(X509 Names)
     * @param caPriKey 用于进行签名的 CA 私钥.
     * @param caNotBefore 证书生效时间, 在这个时间之前证书也是失效的.
     * @param caNotAfter 证书失效时间, 过了这个时间后证书即失效.
     * @param serverPubKey 服务端证书公钥.
     * @param hosts 证书所属域名.
     * @return 返回指定域名所属的服务端 X509 证书.
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
     * 生成 CA 证书(自签名).
     * @param subject 元数据(X509 Names)
     * @param caNotBefore 证书生效时间, 在这个时间之前证书也是失效的.
     * @param caNotAfter 证书失效时间, 过了这个时间后证书即失效.
     * @param keyPair RSA 密钥对.
     * @return 返回自签名 CA 证书.
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
