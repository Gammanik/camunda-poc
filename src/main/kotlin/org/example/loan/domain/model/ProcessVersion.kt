package org.example.loan.domain.model

enum class ProcessVersion(val bpmnProcessId: String) {
    V1("loan-process-v1"),
    V2("loan-process-v2")
}
