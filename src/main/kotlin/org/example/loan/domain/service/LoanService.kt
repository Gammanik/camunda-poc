package org.example.loan.domain.service

import io.camunda.zeebe.client.ZeebeClient
import jakarta.persistence.EntityNotFoundException
import org.example.loan.domain.model.LoanApplication
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.model.ProcessVersion
import org.example.loan.domain.repository.LoanApplicationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class LoanService(
    private val loanApplicationRepository: LoanApplicationRepository,
    private val stateTransitionService: StateTransitionService,
    private val rolloutService: RolloutService,
    private val zeebeClient: ZeebeClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createAndStartProcess(
        applicantId: String,
        amount: BigDecimal,
        productType: String
    ): LoanApplication {
        // Select process version based on rollout config
        val processVersion = rolloutService.selectVersion(productType)

        // Create loan application entity
        val loanApplication = LoanApplication(
            applicantId = applicantId,
            amount = amount,
            productType = productType,
            processVersion = processVersion,
            currentState = LoanState.PENDING_CREDIT_CHECK
        )

        val saved = loanApplicationRepository.save(loanApplication)

        logger.info(
            "Created loan application {} for applicant {} with amount {} on version {}",
            saved.id, applicantId, amount, processVersion
        )

        // Start Zeebe process instance
        val processInstance = zeebeClient.newCreateInstanceCommand()
            .bpmnProcessId(processVersion.bpmnProcessId)
            .latestVersion()
            .variables(
                mapOf(
                    "loanApplicationId" to saved.id!!,
                    "applicantId" to applicantId,
                    "amount" to amount.toString(),
                    "productType" to productType
                )
            )
            .send()
            .join()

        // Update with process instance key
        saved.processInstanceKey = processInstance.processInstanceKey
        loanApplicationRepository.save(saved)

        logger.info(
            "Started process instance {} for loan application {}",
            processInstance.processInstanceKey, saved.id
        )

        // Record initial state transition
        stateTransitionService.recordTransition(
            loanApplication = saved,
            fromState = LoanState.PENDING_CREDIT_CHECK,
            toState = LoanState.PENDING_CREDIT_CHECK,
            actor = "SYSTEM",
            notes = "Loan application created and process started"
        )

        return saved
    }

    @Transactional
    fun updateState(
        loanApplicationId: Long,
        newState: LoanState,
        actor: String = "SYSTEM",
        notes: String? = null
    ): LoanApplication {
        val loanApplication = loanApplicationRepository.findById(loanApplicationId)
            .orElseThrow { EntityNotFoundException("Loan application not found: $loanApplicationId") }

        val oldState = loanApplication.currentState
        loanApplication.currentState = newState
        val updated = loanApplicationRepository.save(loanApplication)

        // Record state transition
        stateTransitionService.recordTransition(
            loanApplication = updated,
            fromState = oldState,
            toState = newState,
            actor = actor,
            notes = notes
        )

        logger.info(
            "Updated loan application {} state: {} -> {}",
            loanApplicationId, oldState, newState
        )

        return updated
    }

    fun findById(id: Long): LoanApplication {
        return loanApplicationRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Loan application not found: $id") }
    }

    fun findByProcessInstanceKey(processInstanceKey: Long): LoanApplication? {
        return loanApplicationRepository.findByProcessInstanceKey(processInstanceKey)
    }

    fun findByCurrentState(currentState: LoanState): List<LoanApplication> {
        return loanApplicationRepository.findByCurrentState(currentState)
    }
}
