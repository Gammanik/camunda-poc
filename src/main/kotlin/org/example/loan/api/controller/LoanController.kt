package org.example.loan.api.controller

import io.camunda.zeebe.client.ZeebeClient
import jakarta.validation.Valid
import org.example.loan.api.dto.LoanApplicationRequest
import org.example.loan.api.dto.LoanStatusResponse
import org.example.loan.api.dto.StateTransitionDto
import org.example.loan.api.dto.UnderwritingDecision
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.service.LoanService
import org.example.loan.domain.service.StateTransitionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/loan")
class LoanController(
    private val loanService: LoanService,
    private val stateTransitionService: StateTransitionService,
    private val zeebeClient: ZeebeClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/apply")
    fun applyForLoan(
        @Valid @RequestBody request: LoanApplicationRequest
    ): ResponseEntity<LoanStatusResponse> {
        logger.info("Received loan application: applicantId={}, amount={}, productType={}",
            request.applicantId, request.amount, request.productType)

        val loanApplication = loanService.createAndStartProcess(
            applicantId = request.applicantId,
            amount = request.amount,
            productType = request.productType
        )

        val response = LoanStatusResponse(
            id = loanApplication.id!!,
            applicantId = loanApplication.applicantId,
            amount = loanApplication.amount,
            productType = loanApplication.productType,
            currentState = loanApplication.currentState,
            processInstanceKey = loanApplication.processInstanceKey,
            processVersion = loanApplication.processVersion,
            creditScore = loanApplication.creditScore,
            contractId = loanApplication.contractId,
            createdAt = loanApplication.createdAt,
            updatedAt = loanApplication.updatedAt
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}/status")
    fun getLoanStatus(@PathVariable id: Long): ResponseEntity<LoanStatusResponse> {
        logger.debug("Getting status for loan {}", id)

        val loanApplication = loanService.findById(id)

        val response = LoanStatusResponse(
            id = loanApplication.id!!,
            applicantId = loanApplication.applicantId,
            amount = loanApplication.amount,
            productType = loanApplication.productType,
            currentState = loanApplication.currentState,
            processInstanceKey = loanApplication.processInstanceKey,
            processVersion = loanApplication.processVersion,
            creditScore = loanApplication.creditScore,
            contractId = loanApplication.contractId,
            createdAt = loanApplication.createdAt,
            updatedAt = loanApplication.updatedAt
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/pending-reviews")
    fun getPendingReviews(): ResponseEntity<List<LoanStatusResponse>> {
        logger.debug("Getting pending manual reviews")

        val pendingLoans = loanService.findByCurrentState(LoanState.MANUAL_REVIEW)

        val response = pendingLoans.map { loanApplication ->
            LoanStatusResponse(
                id = loanApplication.id!!,
                applicantId = loanApplication.applicantId,
                amount = loanApplication.amount,
                productType = loanApplication.productType,
                currentState = loanApplication.currentState,
                processInstanceKey = loanApplication.processInstanceKey,
                processVersion = loanApplication.processVersion,
                creditScore = loanApplication.creditScore,
                contractId = loanApplication.contractId,
                createdAt = loanApplication.createdAt,
                updatedAt = loanApplication.updatedAt
            )
        }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/underwrite")
    fun completeUnderwriting(
        @PathVariable id: Long,
        @Valid @RequestBody decision: UnderwritingDecision
    ): ResponseEntity<LoanStatusResponse> {
        logger.info("Completing manual underwriting for loan {}: decision={}", id, decision.decision)

        val loanApplication = loanService.findById(id)

        // Note: User Task Query API is only available in Zeebe 8.6+
        // For Zeebe 8.5.x, manual underwriting would need to be completed via Operate UI
        // or by using the Tasklist API directly
        logger.warn("User task completion via REST API requires Zeebe 8.6+. Use Operate UI at http://localhost:8081")

        // For now, update the loan state manually for demo purposes
        val newState = if (decision.decision == "approve") LoanState.PENDING_CONTRACT_GENERATION else LoanState.REJECTED
        val updatedLoan = loanService.updateState(
            loanApplicationId = id,
            newState = newState,
            actor = "MANUAL_UNDERWRITER",
            notes = "Manual decision: ${decision.decision} - ${decision.reason ?: "No reason provided"}"
        )

        val response = LoanStatusResponse(
            id = updatedLoan.id!!,
            applicantId = updatedLoan.applicantId,
            amount = updatedLoan.amount,
            productType = updatedLoan.productType,
            currentState = updatedLoan.currentState,
            processInstanceKey = updatedLoan.processInstanceKey,
            processVersion = updatedLoan.processVersion,
            creditScore = updatedLoan.creditScore,
            contractId = updatedLoan.contractId,
            createdAt = updatedLoan.createdAt,
            updatedAt = updatedLoan.updatedAt
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/history")
    fun getLoanHistory(@PathVariable id: Long): ResponseEntity<List<StateTransitionDto>> {
        logger.debug("Getting history for loan {}", id)

        val loanApplication = loanService.findById(id)
        val transitions = stateTransitionService.getHistory(loanApplication)

        val response = transitions.map { transition ->
            StateTransitionDto(
                fromState = transition.fromState,
                toState = transition.toState,
                actor = transition.actor,
                notes = transition.notes,
                timestamp = transition.timestamp
            )
        }

        return ResponseEntity.ok(response)
    }
}
