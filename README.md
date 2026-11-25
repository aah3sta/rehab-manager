# Rehabilitation Center Management System (All 8 modules)

Core Java + JDBC + MySQL, console application.

## Structure
```
sql/schema.sql            all 8 tables + sample staff and patients
src/Main.java             Main menu
src/database/             DatabaseConnection (reusable JDBC connection)
src/models/               Person, Patient, Staff, Doctor, Therapist, Nurse, MedicalAssessment,
                          LabTest, Vitals, TherapySession, MedicationRecord, DischargePlan
src/dao/                  PatientDAO, StaffDAO, MedicalAssessmentDAO, DashboardDAO
                          BaseDAO (shared JDBC code) + LabTestDAO, VitalsDAO,
                          TherapySessionDAO, MedicationDAO, DischargePlanDAO
src/menus/                one console menu per module + MenuSupport (shared prompts)
src/util/InputHelper.java Safe console input (re-asks on invalid input)
src/util/Authorization    "update when authorized" rule shared by all modules
test/IntegrationTest.java 44 checks of every DAO against a real database
```

## Swing dashboard (MainGui)
```
src/MainGui.java              starts the GUI (Main.java still starts the console version)
src/ui/DashboardFrame.java    window: header, sidebar, CardLayout screens, status bar
src/ui/HomePanel.java         counts + today's assessments
src/ui/PatientPanel.java      search / table / form: register, update, delete
src/ui/StaffPanel.java        + role filter, "Show duties" (polymorphism)
src/ui/AssessmentPanel.java   patient / staff / date filters, record, authorized update
src/ui/Refreshable.java       each screen reloads when opened
src/ui/tables/ListTableModel  one generic JTable model for all screens
src/ui/common/UiHelper.java   colours, form layout, date spinners, background DB calls
src/dao/DashboardDAO.java     counts for the home screen
```
- Every DAO call runs in a `SwingWorker` (`UiHelper.runAsync`), so the window never freezes.
- Validation messages come straight from the model setters and show in red under the form.
- Optional: put `flatlaf-3.x.jar` in `lib/` for a more modern look; otherwise Nimbus is used.

Run the GUI:
```
javac -d out $(find src -name "*.java")
java -cp "out:lib/*" -Ddb.password=YOURPASS MainGui
```

## Setup
1. Run the schema: `mysql -u root -p < sql/schema.sql`
2. Put `mysql-connector-j-x.x.x.jar` in a `lib/` folder (download from dev.mysql.com or Maven Central).
3. Compile and run (Linux/macOS; on Windows replace `:` with `;`):
```
javac -d out $(find src -name "*.java")
java -cp "out:lib/*" -Ddb.user=root -Ddb.password=YOURPASS Main
```
In an IDE (IntelliJ/Eclipse/NetBeans), add the connector jar as a library and set the
password in `DatabaseConnection.java` or pass `-Ddb.password=...` as a VM option.

## Business rules
- **Delete patient / remove staff "when permitted"**: refused if they have any linked
  clinical records in any module (also enforced by the foreign keys), and requires y/n confirmation.
- **Who can do what** (methods on Staff, overridden per role):

  | Action | Doctor | Nurse | Therapist | Other roles |
  |---|---|---|---|---|
  | Conduct assessment | yes | yes | yes | no |
  | Record vitals | yes | yes | no | no |
  | Conduct therapy session | no | no | yes | no |
  | Record medication | yes | yes | no | no |

- **"Update when authorized"** (assessments, lab tests, medication): the staff member who
  owns the record, or any Doctor (`util.Authorization`). A lab test's owner is whoever
  conducted its assessment.
- **Lab tests** must belong to an existing assessment; an empty result means "Pending".
- **Vitals**: impossible values are rejected (e.g. temperature outside 30-45 C, systolic not above
  diastolic). Readings outside normal adult ranges are saved but flagged as alerts.
- **Therapy**: duration 5-480 minutes, sessions can't be in the future; total time is a SQL SUM.
- **Medication**: dosage must contain an amount ("500 mg", "2 tablets").
- **Discharge plans**: discharge and follow-up dates are optional (Enter keeps, "-" clears), but a
  follow-up can't be before discharge. Status is In treatment / Discharge planned / Discharged.
- **Record assessment**: patient and staff must exist; the staff member must be a
  Doctor, Therapist or Nurse (a Receptionist cannot conduct one).
- **Update assessment "when authorized"**: only the staff member who conducted it, or a Doctor.
- **Validation**: names are letters only; contact is 10-15 digits (spaces/dashes are stripped);
  gender is MALE/FEMALE/OTHER (M/F/O accepted); date of birth cannot be in the future;
  assessment notes and diagnosis are required.

## Automated test
After loading a fresh `sql/schema.sql`:
```
javac -d out $(find src -name "*.java") && javac -cp out -d out test/IntegrationTest.java
java -cp "out:lib/*" -Ddb.password=YOURPASS IntegrationTest
```
Expected last line: `44 passed, 0 failed`.

## Test checklist (valid + invalid input)
| Operation | Valid test | Invalid test | Confirm in MySQL |
|---|---|---|---|
| Register patient | Brian Mwangi, M, 1990-04-12, 0712345678 | name "R2D2", contact "12345", DOB tomorrow | `SELECT * FROM PATIENT;` |
| Search patient | ID 1; name "mwa"; contact "0712" | ID "abc", ID 999 | — |
| Update patient | change contact | invalid gender "x" | row changed |
| Delete patient | patient with no assessments | patient with assessments | row gone / still there |
| Register staff | role "doctor" → stored "Doctor" | empty role | `SELECT * FROM STAFF;` |
| Staff by role | "nurse" | "pilot" (no results) | — |
| Record assessment | patient 1, staff 1 | staff = Receptionist, patient 999, empty notes | `SELECT * FROM MEDICAL_ASSESSMENT;` |
| Search by date | today's date | "24/11/2026" (wrong format) | — |
| Update assessment | as author or Doctor | as a Nurse who isn't the author | row changed / unchanged |
| Record lab test | under assessment 1, result empty (Pending) | assessment 999 | `SELECT * FROM LAB_TESTS;` |
| Update lab result | as the assessor or a Doctor | as a Therapist who isn't the assessor | RESULT changed / unchanged |
| Record vitals | nurse, 36.8 / 72 / 16 / 5.5 / 120 / 80 | receptionist; temp "abc"; BP 80/90 | `SELECT * FROM VITALS;` |
| Latest vitals | patient with 2+ readings | patient with none | newest row shown |
| Record therapy | therapist, 45 min | nurse as therapist; 2 minutes; future date | `SELECT * FROM THERAPY_SESSION;` |
| Total therapy time | patient with sessions | patient with none (0 min) | `SELECT SUM(DURATION_MINUTES) ...` |
| Record medication | Amoxicillin, 500 mg, Twice daily | dosage "some"; therapist as responsible | `SELECT * FROM MEDICATION_RECORD;` |
| Search medication | "amox" | "zzz" (no results) | - |
| Create plan | goals, instructions, contact, no dates | contact phone "123" | `SELECT * FROM DISCHARGE_AFTERCARE_PLAN;` |
| Plan dates | discharge today, follow-up in 2 weeks | follow-up before discharge | dates changed / unchanged |
| Upcoming follow-ups | after setting a future follow-up | - | soonest first |
