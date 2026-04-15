package org.example.loan.domain.model

enum class LoanState {
    PENDING_CREDIT_CHECK,
    PENDING_DOC_VERIFICATION,
    MANUAL_REVIEW,
    PENDING_CONTRACT_GENERATION,
    COMPLETED,
    REJECTED
}
