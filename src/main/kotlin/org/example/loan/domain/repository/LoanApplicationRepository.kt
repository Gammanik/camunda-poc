package org.example.loan.domain.repository

import org.example.loan.domain.model.LoanApplication
import org.example.loan.domain.model.LoanState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LoanApplicationRepository : JpaRepository<LoanApplication, Long> {
    fun findByProcessInstanceKey(processInstanceKey: Long): LoanApplication?
    fun findByApplicantId(applicantId: String): List<LoanApplication>
    fun findByCurrentState(currentState: LoanState): List<LoanApplication>
}
