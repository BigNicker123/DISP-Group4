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

ProBuild Supplies Ltd is a UK-based medium enterprise that retails and rents building materials and construction tools. The system covers retail sales, tool hire (with external maintenance via FixPro Ltd), warehouse management, the Trade Card loyalty scheme, and interest-free finance through FinTrust UK.

---

## Repository structure

```
.
├── BPMN/
│   ├── Operational Business Process Model.bpmn   # main automated process model
│   └── *.form                                    # 13 Camunda user-task forms
├── Probuild-Project/                             # Spring Boot Camunda 8 job workers
│   ├── src/main/java/.../
│   │   ├── ProcessNameWorker.java                # all 28 @JobWorker handlers
│   │   ├── ApiController.java                    # REST API (bookings, tools, trade cards)
│   │   ├── DataSeeder.java                       # seeds tools and trade card members on startup
│   │   ├── model/                                # Tool, Booking, TradeCard JPA entities
│   │   └── repository/                           # Spring Data repositories
│   └── src/main/resources/application.properties
├── Portfolio/
│   ├── Documents/
│   │   ├── ProjectPlan.md
│   │   └── Testing Report.docx                   # test strategy, 22 test cases, 12 defects
│   └── Socio Technical Model/                    # i* SD and SR models (piStar JSON format)
│       ├── ProBuildSD.txt                        # Strategic Dependency model
│       └── SR Model.txt                          # Strategic Rationale model
├── Strategic Process Model.bpmn                  # AS-IS strategic BPMN
└── README.md
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **Java JDK** | 21+ | Required to build and run the workers |
| **Maven** | 3.8+ | Bundled via `mvnw` wrapper — no install needed |
| **Camunda 8 (self-managed)** | 8.9.0 | Use the c8run bundle (see below) |
| **Camunda Modeler** | 5.44+ | For opening/deploying BPMN and forms |

---

## Running the system

### 1. Start Camunda 8

Use the **c8run bundle** (included in the Camunda 8 Getting Started Bundle):

```bash
# macOS/Linux
"/Applications/camunda8-getting-started-bundle-8.9.0-darwin-aarch64/camunda-start.sh"

# Stop when finished
"/Applications/camunda8-getting-started-bundle-8.9.0-darwin-aarch64/camunda-stop.sh"
```

Wait ~60 seconds for all services to start. Login credentials: **demo / demo**

### 2. Deploy the BPMN and forms

In **Camunda Modeler**:

1. Open `BPMN/Operational Business Process Model.bpmn`
2. Click **Deploy** (rocket icon, bottom left) → target `http://localhost:8080` → Deploy
3. Open and deploy **each `.form` file** in `BPMN/` individually — all 12 forms must be deployed before user tasks will render in Tasklist

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

Startup logs confirm the database has been seeded:
```
Tool catalogue seeded with 27 tools
Trade card members seeded with 5 records
```

### 4. Start a process instance

Go to **Tasklist** (`http://localhost:8080/tasklist`), click **Start process** and select **Customer**.

### 5. Work through the process

Use **Tasklist** to claim and complete user tasks. Use **Operate** (`http://localhost:8080`) to monitor process instances and inspect variable values.

---

## Local URLs — quick reference

| Service | URL | Notes |
|---|---|---|
| **Camunda Tasklist** | http://localhost:8080/tasklist | Complete user tasks here |
| **Camunda Operate** | http://localhost:8080 | Monitor process instances |
| **Camunda Modeler** | Desktop app | Deploy BPMN and forms |
| **H2 Database Console** | http://localhost:9090/h2-console | JDBC URL: `jdbc:h2:mem:probuilddb` — Username: `sa`, Password: *(blank)* |
| **REST API — tools** | http://localhost:9090/api/tools | Full tool catalogue with stock levels |
| **REST API — available tools** | http://localhost:9090/api/tools/available | Tools currently in stock |
| **REST API — bookings** | http://localhost:9090/api/bookings | All hire bookings |
| **REST API — booking by ref** | http://localhost:9090/api/bookings/{reference} | e.g. `/api/bookings/BK-1748123456789` |
| **REST API — bookings by status** | http://localhost:9090/api/bookings/status/{status} | `CONFIRMED` or `RETURNED` |
| **REST API — summary** | http://localhost:9090/api/summary | Booking counts, revenue totals, tool availability |

---

## Database

The application uses an **H2 in-memory database** that is created and seeded automatically on startup. It resets on every restart.

### Tool catalogue (27 tools)

| Category | Examples |
|---|---|
| Power Tools | Cordless Drill, Hammer Drill (SDS), Angle Grinder, Circular Saw, Floor Sander, Paint Sprayer |
| Garden & Outdoor | Strimmer, Chainsaw, Petrol Lawnmower, Rotavator |
| Heavy Equipment | Cement Mixer, Scaffolding, Concrete Breaker, Generator, Air Compressor |
| Access & Safety | Extension Ladder, Podium Steps, Wheelbarrow, Site Lighting |

Each tool has a **purchase price**, **hire rate per day**, **deposit amount**, and **available quantity**. Stock decrements when a hire is confirmed and restores when a tool is returned.

### Trade Card members (5 pre-seeded)

| Card Number | Holder |
|---|---|
| TC-1000000001 | James Builder |
| TC-1000000002 | Sarah Contractor |
| TC-1000000003 | Mike Tradesman |
| TC-1000000004 | Lisa Plumber |
| TC-1000000005 | Dave Electrician |

Trade Card members receive a **10% discount** on purchases and are eligible for **interest-free finance** on purchases of £100 or more.

### Hire bookings

Persisted to the `bookings` table with full customer details (name, email, phone), hire dates, itemised cost breakdown, and status (`CONFIRMED` → `RETURNED`). Booking references are prefixed `BK-`; purchase order references are prefixed `PO-`.

---

## Process overview

The operational BPMN models a multi-party collaboration across four pools:

| Pool | Responsibilities |
|---|---|
| **Customer** | Browse tools, select purchase or hire, payment, delivery, tool returns |
| **FinTrust UK** | Credit application processing, finance decisions, repayment schedule setup |
| **ProBuild LTD** | Tool hire team, logistics (delivery/collection), warehouse operations |
| **FixPro Ltd** | External tool maintenance, inspection, and repair |

### Key flows

**Purchase flow:** Customer selects tools → pays (card/cash/finance) → delivery or click-and-collect → ProBuild packages and delivers → customer confirms receipt → tool returned → ProBuild inspects → FixPro services.

**Hire flow:** Customer selects tools and dates → ProBuild checks availability and confirms booking (saves to DB, decrements stock) → sends booking confirmation → customer completes details and payment → tool delivered → customer returns tool → ProBuild warehouse inspection → tools sent to FixPro → FixPro inspects and services → tools returned to ProBuild → stock restored.

**Finance flow:** Customer requests interest-free finance (Trade Card + purchase ≥ £100) → FinTrust UK processes credit application → approves/declines → if approved, repayment schedule set up (6 or 12 months) → customer notified.

**Repair flow:** ProBuild warehouse inspector marks tool as requiring repair → FixPro technician inspects and classifies as routine/repair/decommission → if repair cost > £50, manager authorisation is flagged → FixPro requests authorisation from ProBuild → ProBuild approves → repair completed → tools returned.

---

## Business rules implemented

| Rule | Implementation |
|---|---|
| Trade Card 10% discount | `calculatePrice` applies discount after DB validation; `originalAmount` and `discountAmount` shown on payment form |
| Finance minimum £100 | `processCreditApplication` declines applications below £100 with reason shown to customer |
| Finance maximum £5,000 | `processCreditApplication` declines applications above £5,000 |
| Repair authorisation > £50 | `sendRepairAuthorisation` flags `requiresManagerAuth = true` and logs requirement when repair cost exceeds £50 |
| Trade card validation | `calculatePrice` validates submitted card number against `TRADE_CARDS` table; invalid cards forfeit discount |
| Stock management | Stock decrements on hire confirmation, restores on tool return |

---

## Camunda forms (13)

| Form file | Used by | Purpose |
|---|---|---|
| `instore-or-online.form` | Customer | Channel selection — Online or In Store |
| `select-tool.form` | Customer | Tool selection (loaded live from DB) and purchase-or-hire choice |
| `duration-of-hire.form` | Customer | Hire dates plus name, email, phone (pre-fills collect-customer-information) |
| `collect-customer-information.form` | Customer | Full customer details with validation; Trade Card number verified against DB |
| `get-payment-details.form` | Customer | Payment method — cash, card (4-digit), or finance; shows original price, discount, final total |
| `choose-delivery-method.form` | Customer | Home delivery vs click-and-collect |
| `select-delivery-time.form` | Customer | Preferred delivery date, time slot, and delivery contact |
| `hire-summary.form` | Customer | Read-only booking confirmation with reference, costs, and dates |
| `pay-probuild-amount.form` | FinTrust UK | Finance amount (pre-filled), repayment period selection, bank details |
| `schedule-delivery.form` | ProBuild Logistics | Order ID (pre-filled), assign delivery van and driver |
| `update-system-delivered.form` | ProBuild Logistics | Confirm actual delivery date and outcome |
| `inspect-tool-handover.form` | ProBuild Warehouse | Tool condition, service type, fault description, estimated repair cost, supervisor sign-off |
| `fixpro-inspection.form` | FixPro Ltd | Technician inspection — confirm or override service classification (routine/repair/decommission) |

---

## Job workers (28)

All service tasks are handled by `ProcessNameWorker.java`:

| Worker type | Pool | Description |
|---|---|---|
| `checkOnlineAvailability` | Customer | Queries DB stock; loads `availableTools` list for Select Tools form |
| `checkInStoreAvailability` | Customer | Queries in-store stock; loads `availableTools` list |
| `calculatePrice` | Customer | Calculates purchase total from DB prices; validates trade card; applies 10% discount |
| `registerMember` | Customer | Issues new Trade Card (TC-XXXXXXXXXX format), persists to DB |
| `processPayment` | Customer | Processes card or cash payment |
| `sendCreditApplication` | Customer | Sends finance request to FinTrust UK with `customerProcessKey` |
| `sendPurchaseDetail` | Customer | Sends purchase to ProBuild warehouse; generates `PO-` order reference if none exists |
| `sendDeliveryRequirements` | Customer | Sends delivery request to ProBuild logistics |
| `sendHireRequest` | Customer | Sends hire booking request to ProBuild hire team |
| `sendReturn` | Customer | Notifies ProBuild of tool return |
| `processCreditApplication` | FinTrust UK | Approves/declines finance (£100–£5,000 range); sets `declineReason` on rejection |
| `logApproved` | FinTrust UK | Logs approval; sets `financeAmount` for the repayment form |
| `logDeclined` | FinTrust UK | Logs decline |
| `setupRepaymentSchedule` | FinTrust UK | Calculates monthly payment for 6 or 12-month term |
| `sendRepaymentPlan` | FinTrust UK | Sends repayment schedule to customer |
| `sendFinanceDecision` | FinTrust UK | Sends approval/decline decision back to Customer pool |
| `checkToolAvailability` | ProBuild Hire | Checks hire tool stock against DB for requested dates |
| `confirmHireBooking` | ProBuild Hire | Calculates hire cost and deposit; saves booking to DB; decrements stock; sets `orderId` |
| `sendBookingConfirmed` | ProBuild Hire | Sends booking confirmation with all details (including customer info) to Customer |
| `sendToolUnavailable` | ProBuild Hire | Notifies customer when requested tool is out of stock |
| `notifyCustomer` | ProBuild Logistics | Sends delivery outcome notification to Customer pool with full context for return flow |
| `notifyPickupReady` | ProBuild Logistics | Notifies customer that click-and-collect item is ready |
| `toolsForMaintenance` | ProBuild Warehouse | Sends tools to FixPro Ltd for servicing |
| `markToolAvailable` | ProBuild Warehouse | Restores tool stock on return; sets booking status to `RETURNED` |
| `sendApproval` | ProBuild Warehouse | Sends repair approval to FixPro |
| `sendDisapproval` | ProBuild Warehouse | Sends repair rejection to FixPro |
| `sendRepairAuthorisation` | FixPro Ltd | Requests repair authorisation; flags `requiresManagerAuth = true` if cost > £50 |
| `sendToolsBack` | FixPro Ltd | Returns serviced tools to ProBuild via `toolsReturned` message |

---

## Deliverables map

| Portfolio criterion | Location |
|---|---|
| i* Socio-technical model (SD + SR) | `Portfolio/Socio Technical Model/` |
| Strategic BPMN | `Strategic Process Model.bpmn` |
| Operational BPMN | `BPMN/Operational Business Process Model.bpmn` |
| Forms / UIs | `BPMN/*.form` (13 forms) |
| Java job workers | `Probuild-Project/src/main/java/` |
| Database (H2 + REST API) | `Probuild-Project/src/main/java/.../model/`, `ApiController.java` |
| Testing report | `Portfolio/Documents/Testing Report.docx` |
| Configuration management | This Git repository |

---

## Notes

- The H2 database resets on every app restart — bookings and stock changes are cleared. This is intentional for the demo environment.
- All forms use Camunda 8.8.0 schema and have been validated in Camunda Modeler 5.46+.
- The Java workers use the Camunda Client SDK (`io.camunda:camunda-spring-boot-starter:8.8.14`).
- `ModuleInfo/` contains official assignment briefs — do not edit.
- For tutor access: ensure the repository is shared with the full module team before submission.
