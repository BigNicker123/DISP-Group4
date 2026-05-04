# Testing Report — ProBuild Supplies Ltd Process Automation
**Module:** UFCFAF-30-3 Development of Information Systems Project  
**Group:** DISP Group 4  
**System:** Camunda 8 Operational Business Process (ProBuild Supplies Ltd)  
**Date:** May 2026

---

## 1. Introduction

This report documents the test strategy, test cases, results, and defects identified during the development and validation of the ProBuild Supplies Ltd automated business process. The system was built using Camunda 8 (self-managed), Spring Boot 3.4.5 job workers, an H2 in-memory database, and Camunda Forms for user input capture.

The operational process models four participant pools: **Customer**, **FinTrust UK**, **ProBuild LTD**, and **FixPro Ltd**. Testing was required to validate all automated flows, message correlations, data persistence, form validation, and business rule enforcement.

---

## 2. Test Strategy

### 2.1 Approach

A **manual integration testing** approach was adopted, supported by inspection-based verification of business logic in the Java job workers. Given the nature of the system — a BPMN process automation engine with human task forms and asynchronous message passing — automated unit testing of individual workers was supplemented by end-to-end process walkthroughs in Camunda Tasklist and Operate.

Testing was conducted iteratively throughout development. Defects identified during testing were fixed and re-tested before progression.

### 2.2 Test Types Used

| Type | Description                                                                  | Tools |
|---|------------------------------------------------------------------------------|---|
| **Integration testing** | End-to-endmy ot process walkthroughs from start event to end event           | Camunda Tasklist, Camunda Operate |
| **Form validation testing** | Verifying input constraints on all Camunda user task forms                   | Camunda Tasklist (browser) |
| **Business logic testing** | Verifying worker calculations (price, discount, hire cost, finance rules)    | Camunda Operate (process variables), H2 Console |
| **Database persistence testing** | Confirming bookings, tools, and trade cards are correctly stored and updated | H2 Console (`http://localhost:9090/h2-console`), REST API |
| **Message correlation testing** | Verifying cross-pool messages reach the correct process instance             | Camunda Operate (instance inspection) |
| **Regression testing** | Re-running previously failed tests after defect fixes                        | Camunda Tasklist, Operate |

### 2.3 Test Environment

| Component | Version / Detail |
|---|---|
| Camunda 8 (self-managed) | 8.9.0 (c8run bundle) |
| Camunda Modeler | 5.46+ |
| Java | 21 |
| Spring Boot | 3.4.5 |
| Database | H2 in-memory (`jdbc:h2:mem:probuilddb`) |
| OS | macOS (darwin arm64) |

### 2.4 Scope

**In scope:**
- All four BPMN pools and their automated service tasks
- All 11 Camunda user task forms and their input validation
- Message start events and intermediate catch events (cross-pool correlation)
- Database persistence (tool stock, bookings, trade card members)
- Business rules: trade card discount, finance eligibility, repair authorisation threshold

**Out of scope:**
- Load/performance testing (single-user demo environment)
- Security/penetration testing
- External API integration (FinTrust and FixPro are modelled as internal participants)

---

## 3. Test Cases and Results

### 3.1 Purchase Flow — Happy Path

| ID | TC-P01 |
|---|---|
| **Type** | Integration |
| **Description** | Complete online purchase with card payment and home delivery |
| **Preconditions** | Camunda running, workers deployed, process deployed with all forms |
| **Steps** | 1. Start Customer process instance. 2. Select "Online". 3. Select tool (e.g. Cordless Drill), Purchase, qty 1. 4. Enter customer details (no trade card). 5. Select payment method "Card", enter 4-digit card number. 6. Choose "Home Delivery", enter address. 7. Select preferred delivery date and slot. 8. ProBuild: Schedule Delivery (assign van and driver). 9. ProBuild: Update System Delivered. |
| **Expected** | Process completes. Booking appears in Operate as ended. Order ID pre-fills on Schedule Delivery form. |
| **Actual** | Process completed successfully. Delivery confirmed. All variables visible in Operate. |
| **Status** | ✅ Pass |

---

| ID | TC-P02 |
|---|---|
| **Type** | Integration |
| **Description** | Purchase with click-and-collect |
| **Steps** | As TC-P01 but select "Click and Collect" at delivery method step. |
| **Expected** | Delivery address field not shown. Process routes to click-and-collect end event. |
| **Actual** | Delivery address and notes fields correctly hidden. Process routed to correct end. |
| **Status** | ✅ Pass |

---

| ID | TC-P03 |
|---|---|
| **Type** | Business Logic |
| **Description** | Trade card member receives 10% discount |
| **Preconditions** | Trade card TC-1000000001 seeded in DB |
| **Steps** | 1. Start Customer process. 2. Select tool (Concrete Breaker, £350). 3. In Customer Information, check "I am a Trade Card Member", enter TC-1000000001. 4. Proceed to payment. |
| **Expected** | `totalAmount` = £315.00 (£350 less 10%). `discountAmount` = £35.00. Discount field visible on payment form. |
| **Actual** | `totalAmount` = £315.00, `discountAmount` = £35.00 confirmed in Operate variables. Discount field displayed on form. |
| **Status** | ✅ Pass |

---

| ID | TC-P04 |
|---|---|
| **Type** | Business Logic |
| **Description** | Invalid trade card number is rejected — no discount applied |
| **Steps** | 1. Start Customer process. 2. Select tool. 3. Check "I am a Trade Card Member", enter TC-9999999999 (not in DB). |
| **Expected** | `tradeCardValid` = false. `isTradeCardMember` reset to false. No discount applied. |
| **Actual** | Worker log: "Trade card 'TC-9999999999' not found in DB — membership rejected". `discountAmount` = 0. Full price charged. |
| **Status** | ✅ Pass |

---

### 3.2 Finance Flow

| ID | TC-F01 |
|---|---|
| **Type** | Integration |
| **Description** | Trade card member successfully applies for interest-free finance |
| **Preconditions** | Valid trade card TC-1000000002 in DB. Purchase total ≥ £100. |
| **Steps** | 1. Complete customer information with trade card TC-1000000002. 2. Select "Interest-Free Finance" as payment method. 3. Finance section appears — select 12-month repayment, confirm agreement. 4. FinTrust pool: "Pay ProBuild agreed amount" form opens — finance amount pre-filled. 5. Enter bank account reference, agree to repayment schedule. |
| **Expected** | `creditApproved` = true. Finance amount pre-fills on FinTrust form. Repayment schedule calculated and sent to customer. |
| **Actual** | Finance amount pre-filled correctly. `monthlyPayment` calculated. Customer notified. |
| **Status** | ✅ Pass |

---

| ID | TC-F02 |
|---|---|
| **Type** | Business Logic |
| **Description** | Finance application declined for purchase under £100 |
| **Steps** | 1. Select a tool priced under £100 (e.g. Strimmer £65). 2. Select "Interest-Free Finance" as payment method. |
| **Expected** | `creditApproved` = false. `declineReason` = "Finance is only available for purchases of £100 or more". Customer routed back to payment form with decline reason displayed. |
| **Actual** | `creditApproved` = false. Decline banner shown on Get Payment Details form: "Finance is only available for purchases of £100 or more". Customer prompted to choose alternative payment. |
| **Status** | ✅ Pass |

---

| ID | TC-F03 |
|---|---|
| **Type** | Business Logic |
| **Description** | Finance application declined for non-Trade Card member |
| **Steps** | 1. Select a tool ≥ £100. 2. Do not tick Trade Card membership. 3. Select "Interest-Free Finance". |
| **Expected** | Finance option present in dropdown (system allows selection) but `creditApproved` = false as FinTrust rejects non-members. |
| **Actual** | As expected. Note: a future improvement would be to hide the finance option entirely for non-members. |
| **Status** | ✅ Pass (with noted limitation) |

---

### 3.3 Hire Flow

| ID | TC-H01 |
|---|---|
| **Type** | Integration |
| **Description** | Complete tool hire — online booking, delivery, and return |
| **Steps** | 1. Start Customer process. Select Online. Select Hire, choose Cordless Drill, qty 1. 2. Duration of Hire: enter name, email, phone, start date, end date. 3. ProBuild: Confirm Hire Booking (auto). 4. ProBuild: Schedule Delivery — verify Order ID pre-fills. 5. Customer: Hire Summary — verify booking reference and costs shown. 6. Customer: Collect Customer Information — verify name/email/phone pre-filled. 7. Customer: complete address, payment. 8. After hire period: Customer returns tool. ProBuild: Inspect Tool Handover — set condition Good, Service Type Routine Maintenance. 9. Tool sent to FixPro, returns, stock restored. |
| **Expected** | Booking saved to DB. Stock decremented. On return, stock restored. Booking status changes to RETURNED. |
| **Actual** | All steps completed. `orderId` = `bookingReference` pre-filled on Schedule Delivery. Customer info pre-filled in step 6. DB confirmed: stock -1 on booking, +1 on return. Booking status RETURNED. |
| **Status** | ✅ Pass |

---

| ID | TC-H02 |
|---|---|
| **Type** | Integration |
| **Description** | Tool unavailable — customer notified |
| **Preconditions** | Reduce a tool's `available_quantity` to 0 in H2 console before test |
| **Steps** | 1. Start hire flow. Select the tool with 0 stock. |
| **Expected** | `toolAvailable` = false. ProBuild routes to "Tool Unavailable" send task. Customer receives unavailability notification. |
| **Actual** | `checkToolAvailability` returns `toolAvailable` = false. Customer notified. |
| **Status** | ✅ Pass |

---

| ID | TC-H03 |
|---|---|
| **Type** | Business Logic |
| **Description** | Tool returned — repair required, cost ≤ £50 (auto-approved) |
| **Steps** | 1. Complete hire flow to return point. 2. Inspect Tool Handover: set Condition = "Damaged – Repair Required", Service Type = "Repair", Repair Cost = £40. |
| **Expected** | `requiresManagerAuth` = false. Repair approved automatically. FixPro routes to Repair path. |
| **Actual** | `repairCost` = 40, `requiresManagerAuth` = false. Repair approved. FixPro service classification gateway routed to Repair. |
| **Status** | ✅ Pass |

---

| ID | TC-H04 |
|---|---|
| **Type** | Business Logic |
| **Description** | Tool returned — repair required, cost > £50 (manager authorisation flagged) |
| **Steps** | 1. As TC-H03 but set Repair Cost = £120. |
| **Expected** | `requiresManagerAuth` = true. Log records "ProBuild Tool Hire Manager authorisation required". Repair still proceeds (manager auth obtained). |
| **Actual** | Worker log: "Repair cost £120.0 exceeds £50 threshold — ProBuild Tool Hire Manager authorisation required". `requiresManagerAuth` = true in Operate. Process continued. |
| **Status** | ✅ Pass |

---

| ID | TC-H05 |
|---|---|
| **Type** | Integration |
| **Description** | Tool returned for routine maintenance — FixPro servicing cycle |
| **Steps** | 1. Inspect Tool Handover: Condition = "Minor Wear", Service Type = "Routine Maintenance". 2. Confirm handover signed off. 3. Follow FixPro flow through to tools returned. |
| **Expected** | FixPro Service Classification routes to Routine Maintenance path. Tools returned to ProBuild. Stock restored. |
| **Actual** | FixPro routed correctly. `markToolAvailable` restored stock. |
| **Status** | ✅ Pass |

---

### 3.4 Form Validation Testing

| ID | TC-V01 |
|---|---|
| **Type** | Form Validation |
| **Description** | Phone number field rejects alphabetic input |
| **Form** | `collect-customer-information` and `duration-of-hire` |
| **Steps** | Enter "abcdefghij" in Phone Number field and attempt to submit. |
| **Expected** | Validation error displayed. Form cannot be submitted. |
| **Actual** | Pattern `^[0-9+\s\-()]{10,15}$` rejected input. Form blocked. |
| **Status** | ✅ Pass |

---

| ID | TC-V02 |
|---|---|
| **Type** | Form Validation |
| **Description** | Email field rejects invalid format |
| **Form** | `collect-customer-information` |
| **Steps** | Enter "notanemail" and attempt to submit. |
| **Expected** | Pattern validation rejects the input. |
| **Actual** | Form validation triggered. Submission blocked. |
| **Status** | ✅ Pass |

---

| ID | TC-V03 |
|---|---|
| **Type** | Form Validation |
| **Description** | Card number field accepts only exactly 4 digits |
| **Form** | `get-payment-details` |
| **Steps** | 1. Select payment method "Card". 2. Enter "abcd" — expect rejection. 3. Enter "12" — expect rejection (too short). 4. Enter "1234" — expect acceptance. |
| **Expected** | Pattern `^[0-9]{4}$` enforced. Only 4-digit numeric input accepted. |
| **Actual** | "abcd" and "12" both rejected. "1234" accepted. |
| **Status** | ✅ Pass |

---

| ID | TC-V04 |
|---|---|
| **Type** | Form Validation |
| **Description** | Trade card number field enforces format TC-XXXXXXXXXX |
| **Form** | `collect-customer-information` |
| **Steps** | 1. Tick "I am a Trade Card Member". 2. Enter "1000000001" (missing TC- prefix) — expect rejection. 3. Enter "TC-123" (too short) — expect rejection. 4. Enter "TC-1000000001" — expect acceptance. |
| **Expected** | Pattern `^TC-[0-9]{10}$` enforced. |
| **Actual** | First two inputs rejected. "TC-1000000001" accepted. |
| **Status** | ✅ Pass |

---

| ID | TC-V05 |
|---|---|
| **Type** | Form Validation |
| **Description** | Trade card number field hidden when checkbox unchecked |
| **Form** | `collect-customer-information` |
| **Steps** | 1. Do not tick "I am a Trade Card Member". 2. Observe form. |
| **Expected** | Trade card number field not visible. `required` constraint not enforced. |
| **Actual** | Field correctly hidden via `conditional.hide` expression. Form submittable without it. |
| **Status** | ✅ Pass |

---

| ID | TC-V06 |
|---|---|
| **Type** | Form Validation |
| **Description** | Payment form sections show/hide based on payment method |
| **Form** | `get-payment-details` |
| **Steps** | 1. Select "Cash" — expect no card or finance sections. 2. Select "Card" — expect card number section only. 3. Select "Finance" — expect finance section only. |
| **Expected** | Conditional sections correctly toggle per selection. |
| **Actual** | All three states behaved as expected. |
| **Status** | ✅ Pass |

---

### 3.5 Database Persistence Testing

| ID | TC-D01 |
|---|---|
| **Type** | Database |
| **Description** | Tool stock decrements correctly on hire booking confirmation |
| **Steps** | 1. Note stock of Cement Mixer (5) via `GET /api/tools`. 2. Complete hire booking for 1 x Cement Mixer. 3. Re-query API. |
| **Expected** | `available_quantity` = 4 |
| **Actual** | `available_quantity` = 4 confirmed via API and H2 Console. |
| **Status** | ✅ Pass |

---

| ID | TC-D02 |
|---|---|
| **Type** | Database |
| **Description** | Tool stock restored correctly on tool return |
| **Steps** | 1. Continue from TC-D01 (Cement Mixer stock = 4). 2. Complete the hire return and inspection flow. |
| **Expected** | `available_quantity` returns to 5. Booking status = RETURNED. |
| **Actual** | Stock = 5. `SELECT * FROM BOOKINGS` confirms `status = 'RETURNED'`. |
| **Status** | ✅ Pass |

---

| ID | TC-D03 |
|---|---|
| **Type** | Database |
| **Description** | New trade card member persisted to DB on registration |
| **Steps** | 1. Start Customer process. 2. Do not tick existing membership. Tick "Register for Trade Card Membership". 3. Complete flow through to registerMember service task. 4. Query `SELECT * FROM TRADE_CARDS`. |
| **Expected** | New row inserted with generated TC number (format TC-XXXXXXXXXX), holder name, active = true. |
| **Actual** | New row visible in TRADE_CARDS table. TC number format confirmed. |
| **Status** | ✅ Pass |

---

| ID | TC-D04 |
|---|---|
| **Type** | Database / API |
| **Description** | Booking stored with full customer details including phone |
| **Steps** | 1. Complete a hire booking via `duration-of-hire` form (with phone). 2. Query `GET /api/bookings/{reference}`. |
| **Expected** | Booking record contains customerFirstName, customerLastName, customerEmail, customerPhone, hire dates, costs. |
| **Actual** | All fields present in API response. |
| **Status** | ✅ Pass |

---

### 3.6 Message Correlation Testing

| ID | TC-M01 |
|---|---|
| **Type** | Message Correlation |
| **Description** | Hire booking confirmation message routes to correct Customer instance |
| **Steps** | 1. Start two concurrent Customer process instances (two browser tabs). 2. Complete Duration of Hire on instance A. 3. Confirm ProBuild sends booking confirmation. |
| **Expected** | Hire summary appears only in instance A's Tasklist. Instance B unaffected. |
| **Actual** | Correct correlation via `customerProcessKey`. Instance B not affected. |
| **Status** | ✅ Pass |

---

| ID | TC-M02 |
|---|---|
| **Type** | Message Correlation |
| **Description** | FixPro tools-returned message routes back to correct ProBuild instance |
| **Steps** | 1. Complete a full repair cycle through FixPro. 2. Confirm `sendToolsBack` correlates to the originating ProBuild instance. |
| **Expected** | ProBuild process resumes correctly. `markToolAvailable` fires and stock is restored. |
| **Actual** | Correct correlation via `probuildProcessKey`. Stock restored. |
| **Status** | ✅ Pass |

---

## 4. Defects Identified

The following defects were discovered during testing and subsequently resolved.

| ID | Severity | Description | Root Cause | Resolution |
|---|---|---|---|---|
| DEF-01 | High | Finance amount not pre-filling on FinTrust "Pay ProBuild agreed amount" form | `financeAmount` was set by `setupRepaymentSchedule` which runs *after* the user task | `logApproved` worker now reads `totalAmount` and outputs `financeAmount` before the user task opens |
| DEF-02 | High | Order ID field empty on "Schedule Delivery" form | `orderId` variable was never written to the process | `confirmHireBooking` now outputs `orderId = bookingReference` |
| DEF-03 | High | Customer name, email, and phone not pre-filling in "Collect Customer Information" after hire request | The Customer pool ends after sending the hire request; a new process instance starts on confirmation and did not receive the customer variables | `sendBookingConfirmed` now explicitly forwards `customerFirstName`, `customerLastName`, `customerEmail`, `customerPhone` in the message payload |
| DEF-04 | High | Inspect Tool Handover form never shown to user; repair and decommission process paths unreachable | "Inspect Tool and handover checklist" was a service task with `serviceType` hardcoded to "routine", so the form was orphaned and the gateway could never route to repair or decommission | Task converted from service task to user task, linked to `inspect-tool-handover` form |
| DEF-05 | Medium | Trade card number not validated against database | Form had a checkbox for membership but no number field and no DB lookup | Added `tradeCardNumber` field to form with regex validation; `calculatePrice` worker now validates against `TradeCardRepository` |
| DEF-06 | Medium | Finance available for any purchase amount regardless of case study rules | `processCreditApplication` approved all amounts under £5,000 | Added minimum threshold: finance now requires `totalAmount >= £100` |
| DEF-07 | Medium | Repair cost threshold not enforced | `sendRepairAuthorisation` always auto-approved all repairs | Worker now reads `repairCost`; repairs > £50 log manager authorisation requirement and set `requiresManagerAuth = true` |
| DEF-08 | Low | Phone number field accepted any text input | No pattern validation on phone field in `duration-of-hire` form; field absent entirely | Added phone field to `duration-of-hire` with `^[0-9+\s\-()]{10,15}$` pattern |
| DEF-09 | Low | Card number field (last 4 digits) accepted letters | No validation on card number field | Pattern `^[0-9]{4}$` added |
| DEF-10 | Low | Finance and card sections always visible regardless of payment method selection | No conditional show/hide on payment sub-sections | Conditional `hide` expressions added for card section and finance section |
| DEF-11 | Medium | Newly registered Trade Card members could not re-use their card number on a subsequent purchase | `registerMember` generated card numbers using `System.currentTimeMillis()` (13 digits, e.g. TC-1746123456789), but the form pattern `^TC-[0-9]{10}$` enforces exactly 10 digits — causing validation failure on re-entry | Card number generation changed to `1_000_000_000 + (currentTimeMillis % 9_000_000_000)`, guaranteeing exactly 10 digits and matching the form regex |
| DEF-12 | Low | Dead `inspectToolHandover` service worker present in codebase | When "Inspect Tool and handover checklist" was converted from a service task to a user task (DEF-04), the original `@JobWorker(type = "inspectToolHandover")` method was not removed — it would never be triggered but added noise | Dead worker removed from `ProcessNameWorker.java` |

---

## 5. Limitations and Areas for Further Refinement

### 5.1 Known Limitations

**Loyalty points system not implemented**  
The case study describes a points-based loyalty system where points accrue per pound spent and unlock higher discount bands. The current implementation applies a flat 10% Trade Card discount. A full points system would require persistent member accounts, points accumulation per transaction, and tiered discount calculation. This was considered beyond the scope of the demonstration model.

**Finance option visible to non-Trade Card members**  
The "Interest-Free Finance" payment option is displayed to all customers in the dropdown. Non-members who select it are declined by FinTrust UK's `processCreditApplication` worker. A refined version would conditionally hide this option using a FEEL expression based on `isTradeCardMember`.

**H2 in-memory database resets on restart**  
The database is re-created and re-seeded each time the Spring Boot application starts. In a production environment this would be a persistent relational database (e.g. PostgreSQL). This is an intentional choice for the demonstration environment.

**Manual task steps in BPMN not automated**  
Several manual tasks (e.g. "Package Items" in ProBuild, "Inspect tools and report status" in FixPro) represent human activities that cannot be automated. These are modelled correctly as manual tasks and are intended to be completed by operators in Tasklist.

**Single assignee ("demo") for all user tasks**  
All user tasks are assigned to the demo user for simplicity. A production system would use role-based assignment (e.g. warehouse staff, logistics team, FinTrust operator) via Camunda's assignment expressions.

### 5.2 Areas for Further Refinement

- Implement a persistent customer account system with stored loyalty points
- Add a gateway to hide the finance payment option for non-Trade Card members
- Extend the REST API to expose trade card member data and loyalty points
- Replace H2 with a persistent database for production deployment
- Add role-based task assignment aligned to the BPMN swim lane structure
- Introduce boundary timer events for SLA enforcement (e.g. FixPro's 5-day turnaround KPI from the case study)

---

## 6. Summary

| Category | Total Tests | Passed | Failed |
|---|---|---|---|
| Integration (Purchase) | 2 | 2 | 0 |
| Integration (Finance) | 3 | 3 | 0 |
| Integration (Hire / Return) | 5 | 5 | 0 |
| Form Validation | 6 | 6 | 0 |
| Database Persistence | 4 | 4 | 0 |
| Message Correlation | 2 | 2 | 0 |
| **Total** | **22** | **22** | **0** |

All 22 test cases passed following resolution of the 12 defects identified during testing. The system successfully demonstrates automated process execution across four participant pools, correct message correlation between pool instances, database-backed tool availability and booking management, and comprehensive form validation aligned with the ProBuild Supplies Ltd case study requirements.
