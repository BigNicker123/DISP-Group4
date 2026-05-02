# DISP Group 4 — ProBuild Supplies Ltd

Coursework repository for **UFCFAF-30-3 Development of Information Systems Project** (UWE Bristol, 2025–26). This repo contains the socio-technical models, BPMN diagrams, Camunda forms, and supporting artefacts produced by Group 4 for the ProBuild Supplies Ltd case study.

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

ProBuild Supplies Ltd is a UK-based medium enterprise that retails and rents building materials and construction tools. The system being modelled covers retail sales, tool hire (with external maintenance via FixPro Ltd), warehouse management, the Trade Card loyalty scheme, and interest-free finance through FinTrust UK. Full case study in `ModuleInfo/Case Study_New.docx`.

---

## Repository structure

```
.
├── BPMN/
│   ├── Operational Business Process Model.bpmn   # main automated process
│   └── *.form                                    # Camunda forms (12 user-task forms)
├── Portfolio/
│   ├── Documents/
│   │   └── ProjectPlan.md                        # task tracker
│   └── Socio Technical Model/
│       ├── ProBuildSD.txt                        # i* Strategic Dependency model
│       ├── SR Model.txt                          # i* Strategic Rationale model
│       └── SR Model Key Business.txt             # SR model — key business processes
├── ModuleInfo/                                   # assignment briefs and case study
├── Strategic Process Model.bpmn                  # AS-IS strategic BPMN
├── Operational Business Process Model.bpmn       # operational BPMN (root copy)
├── DISP project.bpmn
├── Probuild-Project.zip                          # packaged Camunda project
└── README.md
```

### Camunda forms (`BPMN/*.form`)

| Form | Purpose |
|---|---|
| `instore-or-online.form` | Channel selection |
| `select-tool.form` | Tool selection |
| `duration-of-hire.form` | Hire duration |
| `collect-customer-information.form` | Customer details capture |
| `choose-delivery-method.form` | Delivery vs collection |
| `schedule-delivery.form` | Delivery scheduling |
| `select-delivery-time.form` | Delivery time slot |
| `update-system-delivered.form` | Delivery confirmation |
| `hire-summary.form` | Order summary |
| `get-payment-details.form` | Payment capture |
| `pay-probuild-amount.form` | Payment confirmation |
| `inspect-tool-handover.form` | Handover/inspection checklist |

---

## Prerequisites

- **Camunda Modeler** 5.44 or newer — [download](https://camunda.com/download/modeler/)
- **Camunda 8** runtime (Camunda Cloud SaaS or self-managed) — execution platform version **8.8**
- **Git** — for cloning and version control
- A modern web browser — for the Tasklist / Operate UIs

The BPMN files were authored against **Camunda Cloud 8.8.0** (`modeler:executionPlatform="Camunda Cloud"`).

---

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/<org>/DISP-Group4.git
cd DISP-Group4
```

### 2. Open the BPMN models

In Camunda Modeler, open:

- `Strategic Process Model.bpmn` — strategic AS-IS view
- `BPMN/Operational Business Process Model.bpmn` — operational, automatable process
- Any `.form` file in `BPMN/` to view or edit the user-task UIs

### 3. Deploy to Camunda 8

From Camunda Modeler:

1. Open `BPMN/Operational Business Process Model.bpmn`.
2. Click **Deploy** in the bottom toolbar.
3. Select your cluster (Camunda SaaS) or self-managed endpoint.
4. Deploy each `.form` file the process references the same way.
5. Click **Run** (▶) to start a process instance.
6. Open **Tasklist** to claim and complete the user tasks; open **Operate** to inspect process state.

> Alternatively, unzip `Probuild-Project.zip` and import it as a Camunda Web Modeler project.

---

## Deliverables map

Mapping repo contents to the portfolio marking criteria (`ModuleInfo/UFCFAF-30-3 DISP Portfolio 2025-26 V2.docx`):

| Criterion | Where it lives |
|---|---|
| i* Socio-technical model (SD + SR) | `Portfolio/Socio Technical Model/` |
| Strategic BPMN | `Strategic Process Model.bpmn` |
| Operational BPMN | `BPMN/Operational Business Process Model.bpmn` |
| Forms / UIs | `BPMN/*.form` |
| Automation | Deploy `BPMN/` to Camunda 8 (see above) |
| Testing | _(see Testing section)_ |
| Configuration management | This Git repository |

## Testing

> **TODO** — testing evidence to be added under `Portfolio/Testing/`. The strategy, test cases, results, and any defects should be documented here per the portfolio specification.

## Project plan

Live task tracker: [`Portfolio/Documents/ProjectPlan.md`](Portfolio/Documents/ProjectPlan.md).

---

## Notes

- The repository is configured to sync with the Camunda Web Modeler workspace (set up by Nick).
- `ModuleInfo/` contains the official assignment briefs and case study from the module team — do not edit.
- For tutor access: please ensure the repository is shared with the module team before submission.
