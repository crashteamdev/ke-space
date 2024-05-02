package dev.crashteam.repricer.service

import dev.crashteam.mp.base.Date
import dev.crashteam.mp.base.DateRange
import dev.crashteam.mp.external.analytics.category.ExternalCategoryAnalyticsServiceGrpc
import dev.crashteam.mp.external.analytics.category.GetProductDailyAnalyticsRequest
import dev.crashteam.mp.external.analytics.category.GetProductDailyAnalyticsResponse
import dev.crashteam.repricer.service.error.GrpcIntegrationException
import mu.KotlinLogging
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AnalyticsService {

    private val log = KotlinLogging.logger {}

    @GrpcClient("keAnalyticsService")
    private lateinit var keAnalyticsServiceGrpc: ExternalCategoryAnalyticsServiceGrpc.ExternalCategoryAnalyticsServiceBlockingStub

    fun getCompetitorSales(productId: Long): Long? {
        val currentDate = LocalDate.now()
        val date = Date.newBuilder().setDay(currentDate.dayOfMonth)
            .setYear(currentDate.year)
            .setMonth(currentDate.month.value)
            .build()
        val range = DateRange.newBuilder().setFromDate(date).setToDate(date).build()
        val request = GetProductDailyAnalyticsRequest.newBuilder()
            .setUserId("pKbSkfbXuwVuXKXTgIH2z3YOhYj1")
            .setProductId(productId)
            .setDateRange(range)
            .build()
        val productDailyAnalytics = try {
            keAnalyticsServiceGrpc.getProductDailyAnalytics(request)
        } catch (e: Exception) {
            log.error { "Exception while trying to get product daily analytics cause - ${e.message}" }
            null
        }

        if (productDailyAnalytics == null) return null
        when (productDailyAnalytics.responseCase) {
            GetProductDailyAnalyticsResponse.ResponseCase.SUCCESS_RESPONSE -> {
                return productDailyAnalytics
                    .successResponse.productDailyAnalytics.salesChartList.last()

            }

            GetProductDailyAnalyticsResponse.ResponseCase.ERROR_RESPONSE ->
                throw GrpcIntegrationException("Error response from grpc call for analytics")

            GetProductDailyAnalyticsResponse.ResponseCase.RESPONSE_NOT_SET ->
                throw GrpcIntegrationException("Response not set from grpc call for analytics")

            null -> throw GrpcIntegrationException("Null response from grpc call for analytics")
        }
    }
}