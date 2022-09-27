package dev.crashteam.repricer.job

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash
import dev.crashteam.repricer.client.ke.KazanExpressWebClient
import dev.crashteam.repricer.extensions.getApplicationContext
import dev.crashteam.repricer.service.KeShopItemService
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.random.Random

private val log = KotlinLogging.logger {}

class KeShopItemJob : QuartzJobBean() {

    private val avgHash = AverageHash(64)

    private val pHash = PerceptiveHash(32)

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val kazanExpressClient = applicationContext.getBean(KazanExpressWebClient::class.java)
        val keShopItemService = applicationContext.getBean(KeShopItemService::class.java)
        val categoryId = context.jobDetail.jobDataMap["categoryId"] as? Long
            ?: throw IllegalStateException("categoryId can't be null")
        log.info { "Start category product collect for $categoryId" }
        var offset = context.jobDetail.jobDataMap["offset"] as? Int ?: 0
        while (true) {
            val categoryResponse =
                kazanExpressClient.getCategoryGraphQL(categoryId = categoryId.toString(), limit = 48, offset = 0)
            if (categoryResponse?.items.isNullOrEmpty()) break
            val products = categoryResponse?.items ?: break
            products.mapNotNull { categoryProduct ->
                Thread.sleep(Random.nextLong(50, 500))
                val productInfo = kazanExpressClient.getProductInfo(categoryProduct.catalogCard.productId.toString())
                if (productInfo?.payload == null) {
                    log.warn { "Product info payload can't be empty. productId=${categoryProduct.catalogCard.productId}" }
                    return@mapNotNull null // skip bad product
                }
                val productData = productInfo.payload.data
                keShopItemService.addShopItemFromKeData(productData)
            }
            offset += 48
            context.jobDetail.jobDataMap["offset"] = offset
        }
        context.jobDetail.jobDataMap["offset"] = 0
        log.info { "Complete category product collect for $categoryId" }
    }

    private fun generateFingerprint(byteArray: ByteArray, hashAlgorithm: HashingAlgorithm): String {
        val image = ImageIO.read(ByteArrayInputStream(byteArray))
        return hashAlgorithm.hash(image).hashValue.toString(16).uppercase()
    }

    private fun downloadResource(
        imageUrl: String,
        applicationContext: ApplicationContext
    ): ByteArray {
        val restTemplate = applicationContext.getBean(RestTemplate::class.java)
        return restTemplate.getForObject(imageUrl, ByteArray::class.java)!!
    }
}
