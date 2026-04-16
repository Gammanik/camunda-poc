package org.example.loan.domain.repository

import org.example.loan.domain.model.LoanApplication
import org.example.loan.domain.model.StateTransition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StateTransitionRepository : JpaRepository<StateTransition, Long> {
    fun findByLoanApplicationOrderByTimestampDesc(loanApplication: LoanApplication): List<StateTransition>
}
