# Camunda 8 Loan Origination POC

A production-ready demonstration of Camunda 8 (Zeebe) for loan origination workflows, showcasing:
- Process version coexistence and gradual rollout
- Resilient workers with retry mechanisms
- Manual underwriting with user tasks
- Comprehensive state tracking and audit trails

## Technology Stack

- **Camunda Platform**: 8.8.15
- **Spring Boot**: 3.4.6
- **Kotlin**: 2.2.20
- **PostgreSQL**: 16
- **JDK**: 23
- **Gradle**: 8.14

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                        │
│  POST /api/loan/apply                                    │
│  GET  /api/loan/{id}/status                              │
│  GET  /api/loan/pending-reviews                          │
│  POST /api/loan/{id}/underwrite                          │
│  GET  /api/loan/{id}/history                             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Domain Services                        │
│  LoanService (orchestration)                             │
│  RolloutService (version selection: 80/20 split)         │
│  StateTransitionService (audit logging)                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Zeebe Workflow Engine                   │
│  loan-process-v1: credit-check → contract                │
│  loan-process-v2: credit-check → doc-verify → contract   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Zeebe Job Workers                     │
│  CreditCheckWorker (30% flaky simulation)                │
│  DocVerificationWorker (v2 only, 90% success)            │
│  ContractGenerationWorker                                │
└─────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Start Infrastructure

```bash
# Start Zeebe, Elasticsearch, Operate, and PostgreSQL
docker-compose up -d

# Wait for all services to be healthy (~60 seconds)
docker-compose ps
```

Access Camunda Operate UI: http://localhost:8081 (demo/demo)

### 2. Build and Run Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will:
- Connect to Zeebe at localhost:26500
- Auto-deploy BPMN processes on startup
- Start workers listening for jobs
- Expose REST API on port 8080

### 3. Apply for a Loan

```bash
curl -X POST http://localhost:8080/api/loan/apply \
  -H "Content-Type: application/json" \
  -d '{
    "applicantId": "A12345",
    "amount": 25000,
    "productType": "personal-loan"
  }'
```

Response:
```json
{
  "id": 1,
  "applicantId": "A12345",
  "amount": 25000,
  "currentState": "PENDING_CREDIT_CHECK",
  "processInstanceKey": 2251799813685249,
  "processVersion": "V1",
  "creditScore": null,
  "contractId": null,
  "createdAt": "2026-04-15T10:30:00",
  "updatedAt": "2026-04-15T10:30:00"
}
```

### 4. Check Loan Status

```bash
curl http://localhost:8080/api/loan/1/status
```

### 5. View Audit History

```bash
curl http://localhost:8080/api/loan/1/history
```

## Demo Scenarios

### Scenario 1: Happy Path (Auto-Approval)
Applicant with good credit score (750+) gets automatically approved.

```bash
curl -X POST http://localhost:8080/api/loan/apply \
  -H "Content-Type: application/json" \
  -d '{"applicantId": "GOOD_CREDIT", "amount": 25000, "productType": "personal-loan"}'
```

Expected flow: credit-check → APPROVED → generate-contract → COMPLETED

### Scenario 2: Flaky Credit Check (Auto-Retry)
The credit check worker simulates 30% failure rate. Zeebe automatically retries with backoff.

Watch logs for:
```
WARN: Credit service unavailable - will retry (loan: X)
```

Zeebe retries up to 3 times with PT10S backoff before failing.

### Scenario 3: Manual Review Required
Applicant with borderline credit score (600-679) requires manual underwriting.

```bash
# 1. Apply for loan
curl -X POST http://localhost:8080/api/loan/apply \
  -H "Content-Type: application/json" \
  -d '{"applicantId": "BORDERLINE", "amount": 25000, "productType": "personal-loan"}'

# 2. Check pending reviews
curl http://localhost:8080/api/loan/pending-reviews

# 3. Complete manual underwriting
curl -X POST http://localhost:8080/api/loan/1/underwrite \
  -H "Content-Type: application/json" \
  -d '{"decision": "approve", "reason": "Good employment history"}'
```

### Scenario 4: Version Coexistence
80% of personal loans use v1, 20% use v2 (with doc-verification step).

```bash
# Apply 10 times
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/loan/apply \
    -H "Content-Type: application/json" \
    -d "{\"applicantId\": \"USER_$i\", \"amount\": 25000, \"productType\": \"personal-loan\"}"
done

# Check distribution
# ~8 should be on V1, ~2 on V2
```

### Scenario 5: Kill Worker Mid-Execution
Demonstrates Zeebe's resilience when workers fail.

```bash
# 1. Apply for loan
curl -X POST http://localhost:8080/api/loan/apply \
  -H "Content-Type: application/json" \
  -d '{"applicantId": "RESILIENCE_TEST", "amount": 25000, "productType": "personal-loan"}'

# 2. Watch logs for "Starting credit check for loan X"
# 3. Kill the application (Ctrl+C)
# 4. Restart: ./gradlew bootRun
# 5. Process automatically continues from last checkpoint
```

### Scenario 6: Document Verification Failure (V2 Only)
In v2 processes, doc-verification has 10% failure rate that triggers BPMN error.

```bash
# Apply multiple times to v2 process
# 1 in 10 will fail doc verification and be rejected
curl -X POST http://localhost:8080/api/loan/apply \
  -H "Content-Type: application/json" \
  -d '{"applicantId": "DOC_TEST", "amount": 25000, "productType": "personal-loan"}'
```

### Scenario 7: Camunda Operate UI Exploration
1. Open http://localhost:8081 (demo/demo)
2. Navigate to "Processes" → loan-process-v1
3. Click on a running instance
4. View real-time process state with highlighted active tasks
5. Inspect variables (loanApplicationId, creditScore, etc.)
6. Retry failed jobs manually

## Project Structure

```
src/main/kotlin/org/example/loan/
├── LoanApplication.kt                      # Spring Boot entry point
├── domain/
│   ├── model/
│   │   ├── LoanApplication.kt              # JPA entity
│   │   ├── StateTransition.kt              # Audit log entity
│   │   ├── LoanState.kt                    # enum
│   │   ├── ProcessVersion.kt               # enum
│   │   └── CreditDecision.kt               # sealed class
│   ├── repository/
│   │   ├── LoanApplicationRepository.kt
│   │   └── StateTransitionRepository.kt
│   └── service/
│       ├── LoanService.kt
│       ├── RolloutService.kt
│       └── StateTransitionService.kt
├── workflow/
│   ├── worker/
│   │   ├── CreditCheckWorker.kt            # 30% flaky
│   │   ├── DocVerificationWorker.kt        # v2 only
│   │   └── ContractGenerationWorker.kt
│   └── deployment/
│       └── ProcessDeployer.kt
└── api/
    ├── controller/
    │   └── LoanController.kt
    ├── dto/
    │   ├── LoanApplicationRequest.kt
    │   ├── LoanStatusResponse.kt
    │   ├── UnderwritingDecision.kt
    │   └── StateTransitionDto.kt
    └── exception/
        └── GlobalExceptionHandler.kt
```

## Configuration

### Rollout Percentages (application.yml)
```yaml
rollout:
  personal-loan:
    v1: 80
    v2: 20
  mortgage:
    v1: 100
    v2: 0
```

### Worker Configuration
```yaml
camunda:
  worker:
    defaults:
      max-jobs-active: 32
      timeout: PT5M
    threads: 4
```

## State Machine

```
PENDING_CREDIT_CHECK
    ↓
[Credit Check Worker]
    ↓
    ├─→ REJECTED (score < 600)
    ├─→ MANUAL_REVIEW (600 ≤ score < 680)
    └─→ PENDING_CONTRACT_GENERATION (score ≥ 750)
            ↓
        [Contract Worker]
            ↓
        COMPLETED
```

## Database Schema

### loan_applications
- id (PK)
- applicant_id
- amount
- product_type
- current_state
- process_instance_key (unique, indexed)
- process_version
- credit_score
- contract_id
- created_at
- updated_at

### state_transitions
- id (PK)
- loan_application_id (FK, indexed)
- from_state
- to_state
- actor
- notes
- timestamp (indexed)

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Application Logs
```bash
# Spring Boot application
tail -f logs/application.log

# Zeebe broker
docker-compose logs -f zeebe
```

### Metrics
Micrometer metrics exposed at: http://localhost:8080/actuator/metrics

## Troubleshooting

### "Connection refused to Zeebe"
```bash
# Check Zeebe health
docker-compose logs zeebe | grep "ready to accept"

# Restart Zeebe
docker-compose restart zeebe
```

### "Process not found: loan-process-v1"
- Check ProcessDeployer logs during startup
- Verify .bpmn files exist in src/main/resources/bpmn/
- Restart application to re-deploy

### "Jobs not activating in workers"
- Verify job type matches BPMN: `type="credit-check"`
- Check worker thread pool: `camunda.worker.threads=4`
- Ensure Zeebe broker is healthy

### PostgreSQL Connection Issues
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Connect manually
docker exec -it postgres psql -U loan_user -d loan_db
```

## Next Steps

1. **Add Integration Tests**: Use `zeebe-process-test-extension`
2. **Security**: Add Spring Security for API authentication
3. **Monitoring**: Export metrics to Prometheus/Grafana
4. **Documentation**: Generate OpenAPI/Swagger docs
5. **Performance**: Benchmark worker throughput and optimize

## License

This is a proof-of-concept project for demonstration purposes.
