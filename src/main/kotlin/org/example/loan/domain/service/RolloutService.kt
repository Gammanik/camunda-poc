package org.example.loan.domain.service

import org.example.loan.domain.model.ProcessVersion
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class RolloutService(
    private val rolloutConfig: RolloutConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun selectVersion(productType: String): ProcessVersion {
        val config = rolloutConfig.getProductConfig(productType)
        val v1Percentage = config["v1"] ?: 100
        val v2Percentage = config["v2"] ?: 0

        val randomValue = Random.nextInt(100)
        val selectedVersion = if (randomValue < v1Percentage) ProcessVersion.V1 else ProcessVersion.V2

        logger.debug(
            "Selected version {} for product {} (random: {}, v1: {}%, v2: {}%)",
            selectedVersion, productType, randomValue, v1Percentage, v2Percentage
        )

        return selectedVersion
    }
}

@ConfigurationProperties(prefix = "rollout")
class RolloutConfig {
    val personalLoan: MutableMap<String, Int> = mutableMapOf("v1" to 80, "v2" to 20)
    val mortgage: MutableMap<String, Int> = mutableMapOf("v1" to 100, "v2" to 0)

    fun getProductConfig(productType: String): Map<String, Int> {
        return when (productType.lowercase()) {
            "personal-loan" -> personalLoan
            "mortgage" -> mortgage
            else -> mapOf("v1" to 100, "v2" to 0) // Default to v1 only
        }
    }
}
