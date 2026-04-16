package org.example.loan.workflow.worker

import io.camunda.zeebe.spring.client.annotation.JobWorker
import io.camunda.zeebe.spring.client.annotation.Variable
import org.example.loan.domain.model.CreditDecision
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.service.LoanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class CreditCheckWorker(
    private val loanService: LoanService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @JobWorker(
        type = "credit-check",
        timeout = 60000L,
        maxJobsActive = 10
    )
    fun performCreditCheck(
        @Variable loanApplicationId: Long,
        @Variable applicantId: String
    ): Map<String, Any> {
        logger.info("Starting credit check for loan {} (applicant: {})", loanApplicationId, applicantId)

        // Simulate 30% failure rate for flaky service demonstration
        if (Random.nextInt(100) < 30) {
            logger.warn("Credit service unavailable - will retry (loan: {})", loanApplicationId)
            throw RuntimeException("Credit service temporarily unavailable")
        }

        // Simulate credit scoring (in real world, would call external credit bureau API)
        val creditScore = simulateCreditScore(applicantId)
        logger.debug("Credit score for applicant {}: {}", applicantId, creditScore)

        // Determine credit decision based on score
        val decision = when {
            creditScore >= 750 -> {
                logger.info("Credit check APPROVED for loan {} (score: {})", loanApplicationId, creditScore)
                CreditDecision.Approved
            }
            creditScore >= 600 && creditScore < 680 -> {
                logger.info("Credit check requires MANUAL REVIEW for loan {} (score: {})", loanApplicationId, creditScore)
                CreditDecision.ManualReview
            }
            else -> {
                logger.info("Credit check REJECTED for loan {} (score: {})", loanApplicationId, creditScore)
                CreditDecision.Rejected
            }
        }

        // Update loan application state
        val newState = when (decision) {
            CreditDecision.Approved -> LoanState.PENDING_CONTRACT_GENERATION
            CreditDecision.Rejected -> LoanState.REJECTED
            CreditDecision.ManualReview -> LoanState.MANUAL_REVIEW
        }

        val loanApplication = loanService.findById(loanApplicationId)
        loanApplication.creditScore = creditScore

        loanService.updateState(
            loanApplicationId = loanApplicationId,
            newState = newState,
            actor = "CREDIT_CHECK_WORKER",
            notes = "Credit score: $creditScore, Decision: ${decision.value}"
        )

        logger.info("Credit check completed for loan {}: decision={}, score={}", loanApplicationId, decision.value, creditScore)

        // Return variables to Zeebe process
        return mapOf(
            "creditDecision" to decision.value,
            "creditScore" to creditScore,
            "creditReason" to generateCreditReason(decision, creditScore)
        )
    }

    private fun simulateCreditScore(applicantId: String): Int {
        // Generate deterministic but varied credit scores based on applicantId
        // This ensures same applicant gets same score across retries
        val hash = applicantId.hashCode().toLong()
        val random = Random(hash)
        return 550 + random.nextInt(250) // Range: 550-800
    }

    private fun generateCreditReason(decision: CreditDecision, score: Int): String {
        return when (decision) {
            CreditDecision.Approved -> "Excellent credit history (score: $score)"
            CreditDecision.Rejected -> "Insufficient credit history (score: $score)"
            CreditDecision.ManualReview -> "Borderline credit score requires manual review (score: $score)"
        }
    }
}
