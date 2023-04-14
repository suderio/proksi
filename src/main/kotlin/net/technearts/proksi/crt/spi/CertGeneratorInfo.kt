package net.technearts.proksi.crt.spi

/**
 * 证书生成器注解.
 *
 * 用于标注声明生成器的具体不可变信息.
 *
 * @author LamGC
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CertGeneratorInfo(
    /**
     * 生成器名称, 该名称要求唯一, 不可重复.
     *
     * 当遇到名称相同的不同生成器实例时, 将选择第一个加载的实现.
     * @return 返回生成器唯一名称.
     */
    val name: String
)
