package org.example.loan.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "state_transitions",
    indexes = [
        Index(name = "idx_loan_application_timestamp", columnList = "loan_application_id,timestamp")
    ]
)
class StateTransition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    val loanApplication: LoanApplication,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val fromState: LoanState,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val toState: LoanState,

    @Column(nullable = false)
    val actor: String,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
