# Sec Preferences

## Organization Context

MedStream is a healthcare appointment scheduling and telemedicine platform serving 200+ hospitals. We have HIPAA compliance requirements, a 15-year-old legacy codebase, and are actively modernizing our infrastructure.

---

## Path-Based Rules

### Legacy Code
- Mark all findings in `/src/legacy/v1/` as **WONT_FIX** with note: "Legacy system being replaced Q3 2025 - no new development"
- Reduce severity by 2 levels for findings in `src/com/medstream/legacy/reporting` - this section is behind VPN and only accessible by MFA, trusted users
- Findings in `/src/legacy/admin-panel/` are lower priority - IP restricted to office network (10.0.0.0/8)

### Test & Development
- Mark findings in `/test/`, `/tests/`, `/__tests__/`, `/spec/` as **FALSE_POSITIVE** unless:
  - Weak cryptography (bad practice even in tests)
- Findings in `/src/dev-tools/` and `/scripts/dev/` are **INFO** severity max

Hardcoded credentials in test code are false positives, these don't ship.

### High-Risk Areas  
- Increase severity by 1 level for all findings in `/src/api/patient/` (HIPAA sensitive)

### Third-Party & Vendor Code
- Findings in `/vendor/`, `/third_party/`, `/node_modules/` are **WONT_FIX** - we don't control these
- `/src/integrations/epicehr/` uses vendor SDK - SQL injection warnings are likely **FALSE_POSITIVE** (parameterized internally)
- `/public/lib/` contains minified third-party JS - mark **WONT_FIX** with note to check upstream

---

## Vulnerability Type Preferences

### SQL Injection

### Cross-Site Scripting (XSS)

### Cryptography
- Any use of MD5 or SHA1 for passwords is **CRITICAL** (HIPAA requirement)
- Weak random number generation in session ID creation is **CRITICAL**
- Weak random in our rate-limiting jitter logic is **FALSE_POSITIVE** - doesn't need crypto strength

### XML/XXE
- XML entity expansion in `/src/integrations/*/` should assume external untrusted input

### Deserialization
- Java deserialization findings are **CRITICAL** by default
- Python pickle usage in `/src/ml/model-cache/` is **FALSE_POSITIVE** - internal data only, not user-controlled

### Command Injection  
- Command injection in `/scripts/admin/` is **MEDIUM** - internal tooling, not exposed to users
- Command injection from **internal DTOs** (not extending `EHRMessageDTO`) is **FALSE_POSITIVE**:
  - `OrderRequestDTO`, `AppointmentRequestDTO`, etc. are validated by our API layer
  - These DTOs represent trusted internal data, not external user input
  - Example: `OrderCommandService` using `OrderRequestDTO.getFilePath()` is safe - this DTO is internal
- Command injection from **EHR DTOs** (extending `EHRMessageDTO`) is **CRITICAL**:
  - Data from external EHR systems is untrusted
  - Always check if the DTO extends `EHRMessageDTO` to determine trust level

---

## Framework & Technology Context

### Backend (Java Spring Boot)
- We use Spring Security with method-level annotations - findings with `@PreAuthorize` are reduced severity
- Code annotated with `@RateLimited` has reduced attack surface
- Findings in `@RestController` methods are higher priority than internal services


---

## Data Sensitivity Classification

### Always Sensitive (HIPAA PHI)
Variables or fields matching these patterns are **always sensitive**:
- `*patient*` (patient_id, patientName, etc.)
- `*medical*`, `*diagnosis*`, `*prescription*`
- `*ssn*`, `*social_security*`
- `*dob*`, `*date_of_birth*`
- `*address*` (when associated with patients)
- Any field in `patient_records`, `appointments`, `medical_history` tables

### Sensitive (PCI if applicable)
- `*card*`, `*credit*`, `*payment*`
- `*cvv*`, `*billing*`

### NOT Sensitive (Public Data)
- Doctor names and credentials - these are public on our website
- Facility addresses and phone numbers - public information
- Appointment time slots (without patient association) - just scheduling availability
- Application configuration keys (non-secret) like `DATABASE_HOST`, `CACHE_TTL`

### Context-Dependent
- Email addresses: 
  - Patient emails are **PHI** (HIPAA)
  - Staff/doctor emails are **not sensitive** (public directory)
- Phone numbers:
  - Patient phones are **PHI**
  - Facility main lines are **public**
- User IDs:
  - Patient UUIDs are **sensitive** (PII)
  - Internal user IDs are **not sensitive**

---

## Taint Source Policy

### Trusted Sources
These are considered **trusted** in our environment:
- Environment variables (we control deployment via AWS ECS)
- Configuration files loaded at startup (immutable container images)
- Data from our internal Redis cache (written only by our services)
- CLI arguments in `/scripts/admin/` (internal tooling, not exposed)
- **Internal DTOs** (Data Transfer Objects) that do NOT extend `EHRMessageDTO`:
  - `OrderRequestDTO` - internal order processing, validated by our API layer
  - `AppointmentRequestDTO` - internal scheduling, validated by our API layer
  - Any DTO in `com.medstream.api.*` packages that doesn't extend `EHRMessageDTO`
  - These DTOs represent data that has already passed through our validation layer

### Untrusted Sources  
These are **always untrusted**:
- Data from external APIs (EHR integrations, insurance verification)
- **Any class that extends `EHRMessageDTO`** could contain untrusted data from external EHR systems:
  - `PatientDemographicsDTO extends EHRMessageDTO` - untrusted (from Epic/Cerner)
  - `LabResultDTO extends EHRMessageDTO` - untrusted (from external labs)
  - Check class hierarchy: if a DTO extends `EHRMessageDTO`, treat as untrusted
- Direct HTTP request parameters or form data before validation

### Context-Dependent
- Database query results:
  - From `config` or `settings` tables: **trusted**
  - From `patient_records`, `appointments`: **untrusted** (contains user input)
- Message queue data:
  - From our internal services: **semi-trusted** (validate but don't sanitize)
  - From third-party webhooks: **untrusted**

---

## Authentication & Authorization Context

### Reduces Risk
- Methods annotated with `@PreAuthorize("hasRole('ADMIN')")` - reduce severity by 1 level (smaller attack surface)
- Endpoints under `/api/internal/*` with IP whitelist - reduce severity by 1 level

### Special Cases
- Admin panel (`/admin/**`) is IP-restricted to `10.0.0.0/8` - consider in risk assessment
- Patient portal (`/portal/**`) is public-facing - maximum scrutiny
- Mobile API (`/api/mobile/**`) has additional rate limiting - note in triage

---

## Fix Method Preferences

### XSS Prevention
- **Prefer**: DOMPurify for HTML sanitization (we have it configured correctly)
- **Prefer**: React's default escaping over manual escaping
- **Avoid**: Manual regex-based sanitization (brittle and incomplete)
- **Never suggest**: Disabling React's escaping - we don't do this

### SQL Injection Prevention
- **Prefer**: JPA/Hibernate methods (already parameterized)
- **Prefer**: Our internal `QueryBuilder.safe()` utility class
- **Acceptable**: JDBC `PreparedStatement` with proper parameterization
- **Avoid**: String concatenation with "safe" prefixes - still risky

### Path Traversal Prevention
- **Prefer**: Our internal `FilePathValidator.sanitize()` method (already deployed)
- **Acceptable**: Java NIO `Path.normalize()` + whitelist validation
- **Avoid**: Regex-based path validation - easily bypassed

### Cryptography
- **Use**: Argon2 for password hashing (our standard), dont recommend anything else

---

## SCA/Dependency Policy

### General Rules
- Prototype pollution in `devDependencies` is **INFO** severity (not in production bundle)
- Transitive dependencies 3+ levels deep are **WONT_FIX** until direct dependency updates
- CVEs with "PoC available" should be **elevated severity** (+1 level)
- CVEs in test-only dependencies are **reduced severity** (-1 level)

### Known Constraints
- We **cannot upgrade** `lodash@3.x` in legacy code - mark lodash CVEs as **WONT_FIX** with note "blocked by legacy dependencies, migrating to lodash@4.x in Q3 2025"
- We **cannot upgrade** `log4j` in `/src/legacy/` - already on 2.17.1 (safe version), ignore further upgrade suggestions
- `spring-boot-starter-web` must stay at 2.x (not 3.x) due to Java 8 constraint - note this in CVE triage

### Specific Libraries
- `jackson-databind` CVEs: carefully review - we heavily use this for JSON, true vulns are **CRITICAL**
- `commons-collections` deserialization: **CRITICAL** - we use this in multiple places
- `moment.js` in frontend: **WONT_FIX** - deprecated but works fine for our use case, no security impact
- `axios` SSRF issues: **HIGH** - we make many external API calls

---

## Business Logic & Deployment Context

### Application Type
- MedStream is a **multi-tenant SaaS** - data isolation issues are **CRITICAL**
- We serve **HIPAA-regulated data** - privacy violations are **CRITICAL**
- This is **not an internal tool** - assume internet-facing attack surface

### Infrastructure
- Deployed on **AWS ECS behind ALB**
- ALB has **AWS WAF** with XSS and SQL injection rulesets - note this mitigates some attack vectors
- We use **AWS Secrets Manager** for credentials
- Rate limiting is enforced at ALB + application level - DDoS concerns are reduced

### Scale & Risk
- We serve **200+ hospitals**, **50,000+ daily active users**
- Data breach could cost millions (HIPAA fines + reputation)
- Downtime costs ~$50k/hour (contractual SLAs)

---

## Compliance & Regulatory Requirements

### HIPAA (Critical - Enforced by Law)
- All cryptographic weaknesses involving PHI are **CRITICAL**
- Logging PHI to unsecured logs is **CRITICAL** (violation)
- Missing access controls on PHI endpoints are **CRITICAL**
- Insufficient audit logging on PHI access is **HIGH**

### SOC2 (Required for Customer Contracts)
- Weak password validation is **CRITICAL** - we have specific controls
- Missing MFA enforcement on admin accounts is **HIGH**
- Insufficient logging is **MEDIUM** - required for compliance

### PCI-DSS (Applicable to Payment Processing)
- Payment card data must be encrypted - findings in `/src/payments/` are **CRITICAL**
- We use Stripe for actual processing - PCI findings in our code are **reduced severity** (limited scope)

---

### Multi-Factor Analysis
If a finding matches multiple rules with conflicting severity:
1. **HIPAA/Compliance rules override** everything
2. **Path-based rules** (high-risk areas) take precedence over general rules
3. **Vulnerability type** rules are default baseline
4. Apply modifiers (auth context, frameworks) last

## Version History
- v1.0 - 2025-01-15 - Initial version (Security Team)
- v1.1 - 2025-02-20 - Added SCA policy and fix preferences (DevOps Team)
- v1.2 - 2025-03-10 - Refined HIPAA classification (Compliance Team)

---

*This file should be reviewed quarterly and updated as our security posture evolves.*

