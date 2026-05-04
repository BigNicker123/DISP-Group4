# DISP Group 4 — ProBuild Supplies Ltd

Coursework repository for **UFCFAF-30-3 Development of Information Systems Project** (UWE Bristol, 2025–26). This repo contains the socio-technical models, BPMN diagrams, Camunda forms, Java job workers, and supporting artefacts produced by Group 4 for the ProBuild Supplies Ltd case study.

---

## Module details

| | |
|---|---|
| **Module** | UFCFAF-30-3 — Development of Information Systems Project |
| **Module leader** | Steve Battle |
| **Tutors** | Dilshan Jayatilake, Steve Battle, Ryan Fellows |
| **Year** | 2025–26 |
| **Portfolio submission** | 23/04/2026 (70%) |
| **Presentation submission** | 30/04/2026 (30%) |

## Team

- Nick
- Sam
- Wilson
- Nathan
- Raees

## Case study (in brief)

ProBuild Supplies Ltd is a UK-based medium enterprise that retails and rents building materials and construction tools. The system covers retail sales, tool hire (with external maintenance via FixPro Ltd), warehouse management, the Trade Card loyalty scheme, and interest-free finance through FinTrust UK. Full case study in `ModuleInfo/Case Study_New.docx`.

---

## Repository structure

```
.
├── BPMN/
│   ├── Operational Business Process Model.bpmn   # main automated process model
│   └── *.form                                    # 11 Camunda user-task forms
├── Probuild-Project/                             # Spring Boot Camunda 8 job workers
│   ├── src/main/java/.../
│   │   ├── ProcessNameWorker.java                # all 29 @JobWorker handlers
│   │   ├── ApiController.java                    # REST API (bookings + tools)
│   │   ├── DataSeeder.java                       # seeds tool catalogue on startup
│   │   ├── model/                                # Tool and Booking JPA entities
│   │   └── repository/                           # Spring Data repositories
│   └── src/main/resources/application.properties
├── Portfolio/
│   ├── Documents/ProjectPlan.md
│   └── Socio Technical Model/                    # i* SD and SR models
├── ModuleInfo/                                   # assignment briefs and case study
├── Strategic Process Model.bpmn                  # AS-IS strategic BPMN
└── README.md
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **Java JDK** | 21+ | Required to build and run the workers |
| **Maven** | 3.8+ | Bundled via `mvnw` wrapper — no install needed |
| **Camunda 8 (self-managed)** | 8.8 | Local Docker Compose stack |
| **Camunda Modeler** | 5.44+ | For opening/deploying BPMN and forms |
| **Docker Desktop** | Latest | Required to run the Camunda stack |

---

## Running the system

### 1. Start Camunda 8

Start your local Camunda 8 self-managed stack via Docker Compose. Ensure Zeebe, Operate, and Tasklist are all healthy before proceeding.

```bash
docker compose up -d
```

### 2. Deploy the BPMN and forms

In **Camunda Modeler**:

1. Open `BPMN/Operational Business Process Model.bpmn`
2. Click **Deploy** → target `http://localhost:8080`
3. Open and deploy each `.form` file in `BPMN/` the same way — forms must be deployed before user tasks will render in Tasklist

> All forms must be redeployed whenever the BPMN is redeployed, as they use `bindingType: deployment`.

### 3. Start the Java job workers

```bash
cd Probuild-Project
./mvnw spring-boot:run
```

On Windows:
```bash
mvnw.cmd spring-boot:run
```

The workers will connect to Zeebe and begin polling. Startup logs will confirm the H2 database has been seeded with the tool catalogue and that all workers are subscribed.

### 4. Start a process instance

In **Camunda Tasklist** (`http://localhost:8082`), click **Start process** and select **Customer**. Alternatively use Operate to start an instance manually.

### 5. Work through the process

Use **Tasklist** to claim and complete user tasks as they appear. Use **Operate** to monitor process state and variable values.

---

## Local URLs — quick reference

| Service | URL | Notes |
|---|---|---|
| **Camunda Tasklist** | http://localhost:8082 | Complete user tasks here |
| **Camunda Operate** | http://localhost:8081 | Monitor process instances |
| **Camunda Modeler** | Desktop app | Deploy BPMN and forms |
| **H2 Database Console** | http://localhost:9090/h2-console | JDBC URL: `jdbc:h2:mem:probuilddb` — Username: `sa`, Password: *(blank)* |
| **REST API — tools** | http://localhost:9090/api/tools | Full tool catalogue with stock levels |
| **REST API — available tools** | http://localhost:9090/api/tools/available | Tools with stock > 0 |
| **REST API — bookings** | http://localhost:9090/api/bookings | All hire bookings |
| **REST API — booking by ref** | http://localhost:9090/api/bookings/{reference} | e.g. `/api/bookings/BK-1748123456` |
| **REST API — bookings by status** | http://localhost:9090/api/bookings/status/{status} | `CONFIRMED` or `RETURNED` |
| **REST API — summary** | http://localhost:9090/api/summary | Booking counts, revenue totals, tool availability |

---

## Database

The application uses an **H2 in-memory database** that is automatically created and seeded when the Java app starts. It resets on every restart.

The tool catalogue (27 items) is seeded by `DataSeeder.java` across four categories:

| Category | Examples |
|---|---|
| Power Tools | Cordless Drill, Hammer Drill (SDS), Angle Grinder, Circular Saw, Floor Sander, Paint Sprayer |
| Garden & Outdoor | Strimmer, Chainsaw, Petrol Lawnmower, Rotavator |
| Heavy Equipment | Cement Mixer, Scaffolding, Concrete Breaker, Generator, Air Compressor |
| Access & Safety | Extension Ladder, Podium Steps, Wheelbarrow, Site Lighting |

Each tool has a **purchase price**, **hire rate per day**, **deposit amount**, and **available quantity**. Stock is decremented when a hire is confirmed and restored when a tool is returned.

Hire bookings are persisted to the `bookings` table with full customer details, hire dates, cost breakdown, and status (`CONFIRMED` → `RETURNED`).

---

## Process overview

The operational BPMN models a multi-party collaboration across four pools:

| Pool | Responsibilities |
|---|---|
| **Customer** | Browse tools, select purchase or hire, payment, returns |
| **FinTrust UK** | Credit application processing and finance decisions |
| **ProBuild LTD** | Tool hire team, logistics, warehouse operations |
| **FixPro Ltd** | External tool maintenance and repair |

### Key flows

**Purchase flow:** Customer selects tools → pays (card/cash/finance) → ProBuild packages and delivers → customer receives package.

**Hire flow:** Customer selects tools and dates → ProBuild checks availability → confirms booking (saves to DB, decrements stock) → sends confirmation → customer completes details and payment → tool delivered → customer returns tool → ProBuild inspects → sends to FixPro for servicing → FixPro returns tools → ProBuild restores stock.

**Finance flow:** Customer requests interest-free finance → FinTrust UK processes credit application → approves/declines → repayment schedule set up → notifies customer.

---

## Camunda forms

| Form file | Used by | Purpose |
|---|---|---|
| `instore-or-online.form` | Customer | Channel selection — Online or In Store |
| `select-tool.form` | Customer | Tool selection (options loaded live from DB) and purchase-or-hire |
| `duration-of-hire.form` | Customer | Hire dates plus customer name and email |
| `collect-customer-information.form` | Customer | Full customer details with email/phone validation |
| `get-payment-details.form` | Customer | Payment method — cash, card, or finance |
| `choose-delivery-method.form` | Customer | Home delivery vs click-and-collect |
| `select-delivery-time.form` | Customer | Preferred delivery date and time slot |
| `hire-summary.form` | Customer | Read-only booking confirmation with reference, costs, and dates |
| `pay-probuild-amount.form` | FinTrust UK | Finance repayment confirmation and bank details |
| `schedule-delivery.form` | ProBuild Logistics | Assign van and driver for delivery |
| `update-system-delivered.form` | ProBuild Logistics | Confirm actual delivery date and status |

---

## Job workers

All 29 service tasks are handled by `ProcessNameWorker.java`:

| Worker type | Pool | Description |
|---|---|---|
| `checkOnlineAvailability` | Customer | Checks online stock; loads tool catalogue into `availableTools` process variable |
| `checkInStoreAvailability` | Customer | Checks in-store stock; loads tool catalogue into `availableTools` process variable |
| `calculatePrice` | Customer | Calculates total purchase price from DB rates |
| `registerMember` | Customer | Registers customer for Trade Card membership |
| `processPayment` | Customer | Processes card or cash payment |
| `sendCreditApplication` | Customer | Sends finance request to FinTrust UK |
| `sendPurchaseDetail` | Customer | Sends purchase detail to ProBuild warehouse |
| `sendDeliveryRequirements` | Customer | Sends delivery request to ProBuild logistics |
| `sendHireRequest` | Customer | Sends hire booking request to ProBuild hire team |
| `sendReturn` | Customer | Sends tool return notification to ProBuild |
| `processCreditApplication` | FinTrust UK | Auto-approves credit applications under £5,000 |
| `logApproved` | FinTrust UK | Logs approved credit application |
| `logDeclined` | FinTrust UK | Logs declined credit application |
| `setupRepaymentSchedule` | FinTrust UK | Sets up 6 or 12 month repayment schedule |
| `sendRepaymentPlan` | FinTrust UK | Sends repayment plan to customer |
| `sendFinanceDecision` | FinTrust UK | Sends approval or decline back to customer |
| `checkToolAvailability` | ProBuild | Checks hire tool availability against DB stock |
| `confirmHireBooking` | ProBuild | Calculates hire cost and deposit; saves booking to DB; decrements tool stock |
| `sendBookingConfirmed` | ProBuild | Sends booking confirmation with all details to customer |
| `sendToolUnavailable` | ProBuild | Notifies customer when requested tool is out of stock |
| `notifyCustomer` | ProBuild | Sends delivery status notification to customer |
| `notifyPickupReady` | ProBuild | Notifies customer that item is ready to collect |
| `inspectToolHandover` | ProBuild | Records tool inspection result and service classification |
| `toolsForMaintenance` | ProBuild | Sends tools to FixPro Ltd for servicing |
| `markToolAvailable` | ProBuild | Restores tool stock on return; sets booking status to RETURNED |
| `sendApproval` | ProBuild | Approves FixPro repair request |
| `sendDisapproval` | ProBuild | Declines FixPro repair request |
| `sendRepairAuthorisation` | FixPro | Requests repair approval from ProBuild |
| `sendToolsBack` | FixPro | Returns serviced tools to ProBuild |

---

## Deliverables map

| Portfolio criterion | Location |
|---|---|
| i* Socio-technical model (SD + SR) | `Portfolio/Socio Technical Model/` |
| Strategic BPMN | `Strategic Process Model.bpmn` |
| Operational BPMN | `BPMN/Operational Business Process Model.bpmn` |
| Forms / UIs | `BPMN/*.form` |
| Java job workers | `Probuild-Project/src/main/java/` |
| Database (H2 + REST API) | `Probuild-Project/src/main/java/.../model/`, `ApiController.java` |
| Testing | `Portfolio/Testing/` *(in progress)* |
| Configuration management | This Git repository |

---

## Notes

- The H2 database resets on every app restart — all bookings and stock changes are cleared. This is intentional for demo purposes.
- All forms use Camunda 8.8.0 schema and have been validated in Camunda Modeler 5.46+.
- The Java workers use the Camunda Client SDK (`io.camunda:camunda-spring-boot-starter:8.8.14`).
- `ModuleInfo/` contains official assignment briefs — do not edit.
- For tutor access: ensure the repository is shared with the module team before submission.
