package org.example.loan.api.dto

import org.example.loan.domain.model.LoanState
import java.time.LocalDateTime

data class StateTransitionDto(
    val fromState: LoanState,
    val toState: LoanState,
    val actor: String,
    val notes: String?,
    val timestamp: LocalDateTime
)
