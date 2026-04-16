package org.example.loan

import org.example.loan.domain.service.RolloutConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RolloutConfig::class)
class LoanApplication

fun main(args: Array<String>) {
    runApplication<LoanApplication>(*args)
}
