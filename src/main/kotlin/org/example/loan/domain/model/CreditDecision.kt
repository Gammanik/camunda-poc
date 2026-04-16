package org.example.loan.domain.model

sealed class CreditDecision(val value: String) {
    data object Approved : CreditDecision("APPROVED")
    data object Rejected : CreditDecision("REJECTED")
    data object ManualReview : CreditDecision("MANUAL_REVIEW")

    companion object {
        fun fromString(value: String): CreditDecision {
            return when (value.uppercase()) {
                "APPROVED" -> Approved
                "REJECTED" -> Rejected
                "MANUAL_REVIEW" -> ManualReview
                else -> throw IllegalArgumentException("Unknown credit decision: $value")
            }
        }
    }
}
