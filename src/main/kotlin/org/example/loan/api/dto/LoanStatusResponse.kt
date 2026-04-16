package org.example.loan.api.dto

import org.example.loan.domain.model.LoanState
import org.example.loan.domain.model.ProcessVersion
import java.math.BigDecimal
import java.time.LocalDateTime

data class LoanStatusResponse(
    val id: Long,
    val applicantId: String,
    val amount: BigDecimal,
    val productType: String,
    val currentState: LoanState,
    val processInstanceKey: Long?,
    val processVersion: ProcessVersion,
    val creditScore: Int?,
    val contractId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
