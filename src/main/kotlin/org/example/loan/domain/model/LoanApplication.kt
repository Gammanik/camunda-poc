package org.example.loan.domain.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "loan_applications",
    indexes = [
        Index(name = "idx_applicant_id", columnList = "applicantId"),
        Index(name = "idx_process_instance_key", columnList = "processInstanceKey"),
        Index(name = "idx_current_state", columnList = "currentState")
    ]
)
class LoanApplication(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val applicantId: String,

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column(nullable = false)
    val productType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var currentState: LoanState = LoanState.PENDING_CREDIT_CHECK,

    @Column(unique = true)
    var processInstanceKey: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val processVersion: ProcessVersion,

    @Column
    var creditScore: Int? = null,

    @Column
    var contractId: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "loanApplication", cascade = [CascadeType.ALL], orphanRemoval = true)
    val stateTransitions: MutableList<StateTransition> = mutableListOf()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
