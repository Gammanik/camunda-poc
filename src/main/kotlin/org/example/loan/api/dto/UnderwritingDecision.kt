package org.example.loan.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UnderwritingDecision(
    @field:NotBlank(message = "Decision is required")
    @field:Pattern(regexp = "approve|reject", message = "Decision must be 'approve' or 'reject'")
    val decision: String,

    val reason: String? = null
)
