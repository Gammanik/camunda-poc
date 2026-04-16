package org.example.loan.workflow.worker

import io.camunda.zeebe.client.api.response.ActivatedJob
import io.camunda.zeebe.client.api.worker.JobClient
import io.camunda.zeebe.spring.client.annotation.JobWorker
import io.camunda.zeebe.spring.client.annotation.Variable
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.service.LoanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class DocVerificationWorker(
    private val loanService: LoanService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @JobWorker(
        type = "doc-verification",
        timeout = 120000L,
        maxJobsActive = 10
    )
    fun verifyDocuments(
        @Variable loanApplicationId: Long,
        @Variable applicantId: String,
        jobClient: JobClient,
        job: ActivatedJob
    ) {
        logger.info("Starting document verification for loan {} (applicant: {})", loanApplicationId, applicantId)

        // Update state to pending doc verification
        loanService.updateState(
            loanApplicationId = loanApplicationId,
            newState = LoanState.PENDING_DOC_VERIFICATION,
            actor = "DOC_VERIFICATION_WORKER",
            notes = "Document verification in progress"
        )

        // Simulate document verification (in real world, would call document verification service)
        // 90% success rate
        if (Random.nextInt(100) < 10) {
            logger.warn("Document verification FAILED for loan {} - throwing BPMN error", loanApplicationId)

            // Update state to rejected
            loanService.updateState(
                loanApplicationId = loanApplicationId,
                newState = LoanState.REJECTED,
                actor = "DOC_VERIFICATION_WORKER",
                notes = "Document verification failed: Missing required documents"
            )

            // Throw BPMN error to trigger error boundary event
            jobClient.newThrowErrorCommand(job.key)
                .errorCode("DOC_VERIFICATION_FAILED")
                .errorMessage("Required documents are missing or invalid")
                .send()
                .join()

            return
        }

        logger.info("Document verification PASSED for loan {}", loanApplicationId)

        // Document verification successful - complete the job
        jobClient.newCompleteCommand(job.key)
            .variables(
                mapOf(
                    "docVerificationStatus" to "VERIFIED",
                    "docVerificationNotes" to "All required documents verified successfully"
                )
            )
            .send()
            .join()
    }
}
