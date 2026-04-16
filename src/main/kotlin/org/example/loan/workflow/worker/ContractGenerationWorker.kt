package org.example.loan.workflow.worker

import io.camunda.zeebe.spring.client.annotation.JobWorker
import io.camunda.zeebe.spring.client.annotation.Variable
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.service.LoanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ContractGenerationWorker(
    private val loanService: LoanService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @JobWorker(
        type = "generate-contract",
        timeout = 60000L,
        maxJobsActive = 10
    )
    fun generateContract(
        @Variable loanApplicationId: Long,
        @Variable applicantId: String,
        @Variable amount: String
    ): Map<String, Any> {
        logger.info("Starting contract generation for loan {} (applicant: {}, amount: {})", loanApplicationId, applicantId, amount)

        // Generate contract ID
        val contractId = generateContractId(loanApplicationId)
        logger.debug("Generated contract ID: {} for loan {}", contractId, loanApplicationId)

        // Update loan application with contract ID and mark as completed
        val loanApplication = loanService.findById(loanApplicationId)
        loanApplication.contractId = contractId

        loanService.updateState(
            loanApplicationId = loanApplicationId,
            newState = LoanState.COMPLETED,
            actor = "CONTRACT_GENERATION_WORKER",
            notes = "Contract generated successfully: $contractId"
        )

        logger.info("Contract generation completed for loan {}: contractId={}", loanApplicationId, contractId)

        // Return contract ID to process
        return mapOf(
            "contractId" to contractId,
            "contractGeneratedAt" to Instant.now().toString()
        )
    }

    private fun generateContractId(loanApplicationId: Long): String {
        val timestamp = Instant.now().epochSecond
        return "CONTRACT-$loanApplicationId-$timestamp"
    }
}
