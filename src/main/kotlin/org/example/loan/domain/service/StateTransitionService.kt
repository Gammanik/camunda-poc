package org.example.loan.domain.service

import org.example.loan.domain.model.LoanApplication
import org.example.loan.domain.model.LoanState
import org.example.loan.domain.model.StateTransition
import org.example.loan.domain.repository.StateTransitionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StateTransitionService(
    private val stateTransitionRepository: StateTransitionRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun recordTransition(
        loanApplication: LoanApplication,
        fromState: LoanState,
        toState: LoanState,
        actor: String = "SYSTEM",
        notes: String? = null
    ): StateTransition {
        val transition = StateTransition(
            loanApplication = loanApplication,
            fromState = fromState,
            toState = toState,
            actor = actor,
            notes = notes
        )

        val saved = stateTransitionRepository.save(transition)

        logger.info(
            "State transition recorded for loan {}: {} -> {} by {} {}",
            loanApplication.id,
            fromState,
            toState,
            actor,
            if (notes != null) "(notes: $notes)" else ""
        )

        return saved
    }

    fun getHistory(loanApplication: LoanApplication): List<StateTransition> {
        return stateTransitionRepository.findByLoanApplicationOrderByTimestampDesc(loanApplication)
    }
}
