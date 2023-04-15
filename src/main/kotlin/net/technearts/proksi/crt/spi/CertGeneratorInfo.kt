package net.technearts.proksi.crt.spi

/**
 * Info do Gerador de Certificado.
 *
 * Informações imutáveis concretas para geradores de declaração de anotação.
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CertGeneratorInfo(
    /**
     * Nome do gerador, o nome deve ser único e não pode ser repetido.
     *
     * Quando uma instância de gerador diferente com o mesmo nome for encontrada, a primeira implementação carregada será escolhida.
     * @return Retorna o nome exclusivo do gerador.
     */
    val name: String
)
