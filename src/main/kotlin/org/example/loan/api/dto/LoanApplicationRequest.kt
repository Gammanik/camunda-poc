package org.example.loan.api.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class LoanApplicationRequest(
    @field:NotBlank(message = "Applicant ID is required")
    val applicantId: String,

    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,

    @field:NotBlank(message = "Product type is required")
    val productType: String
)
