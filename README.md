# Rehabilitation Centre Management System (RCMS)

## 1. Project Overview

The **Rehabilitation Centre Management System (RCMS)** is a Java-based, console-driven application designed to simplify and centralise the management of operations within a rehabilitation centre.

The system provides a structured way to manage patient information, staff records, medical assessments, laboratory tests, vital signs, therapy sessions, medication records, and patient discharge and aftercare plans.

The project aims to demonstrate the practical application of Object-Oriented Programming (OOP) principles in solving a real-world problem while providing an organised and efficient approach to rehabilitation centre management.

The application will allow authorised users to interact with different management modules through a menu-driven terminal interface, with MySQL serving as the central database for persistent data storage.

## 2. Project Objectives

The main objectives of the project are to:

- Develop a functional rehabilitation centre management application using Java.
- Apply fundamental Object-Oriented Programming principles in a real-world scenario.
- Centralise patient and staff records for easier management and retrieval.
- Provide structured management of medical assessments, laboratory tests, and patient vitals.
- Maintain records of therapy sessions and medication.
- Facilitate patient discharge planning and aftercare follow-ups.
- Implement database connectivity to ensure information is stored and retrieved reliably.

## 3. System Modules and Features

The system is divided into eight main modules, each responsible for a specific area of rehabilitation centre operations.

### 3.1 Patient Management

Responsible for managing patient information throughout their interaction with the rehabilitation centre.

Features:
- Register new patients.
- View all registered patients.
- Search patients by ID, name, or contact.
- Update patient information.
- Delete patient records when permitted.

### 3.2 Staff Management

Handles the registration and management of rehabilitation centre personnel.

Features:
- Register staff members and record their roles and contact information.
- View all registered staff.
- Search staff by ID, name, or contact.
- Filter staff members by role.
- Update or remove staff records when permitted.

### 3.3 Medical Assessment

Manages medical assessments conducted on registered patients.

Features:
- Record medical assessments and associate them with the relevant patient and staff member.
- View assessments by assessment ID.
- Retrieve all assessments belonging to a particular patient.
- Search assessments by date or staff member.
- Update assessment records when authorised.

### 3.4 Laboratory Test Management

Handles laboratory tests associated with medical assessments.

Features:
- Record laboratory tests under existing medical assessments.
- Retrieve laboratory tests by test ID.
- View all tests associated with a particular assessment.
- Retrieve a patient's laboratory test history through their assessments.
- Update test results and notes when authorised.

### 3.5 Vitals Management

Maintains records of patients' vital signs to support monitoring and assessment.

Features:
- Record patient temperature, pulse rate, breathing rate, glucose levels, and blood pressure.
- Retrieve vitals records by ID.
- View all recorded vitals for a patient.
- Retrieve the latest recorded vitals.
- Search records by date or staff member.

### 3.6 Therapy Session Management

Manages therapy activities and tracks patient participation and progress.

Features:
- Record therapy sessions, including date, duration, notes, patient, and responsible staff member.
- Retrieve individual sessions by session ID.
- View a patient's therapy history.
- Search sessions by staff member or date.
- Calculate the total recorded therapy time for a patient.

### 3.7 Medication Management

Maintains medication records for patients receiving treatment at the centre.

Features:
- Record medication name, dosage, frequency, instructions, patient, and responsible staff member.
- Retrieve medication records by ID.
- View a patient's medication history.
- Search medication records by medication name or staff member.
- Update medication details and instructions when authorised.

### 3.8 Discharge and Aftercare Management

Handles patient discharge planning and continued recovery monitoring.

Features:
- Create discharge and aftercare plans for patients.
- Retrieve aftercare plans by plan ID or patient.
- Record and update recovery goals and aftercare instructions.
- Record and update discharge and follow-up dates.
- View upcoming follow-up dates and emergency relapse contacts.

## 4. Technologies and Tools

The project will use the following technologies:

| Technology | Purpose |
|---|---|
| Java (Core Java) | Main programming language and application logic |
| MySQL | Relational database for persistent data storage |
| JDBC | Connecting the Java application to MySQL |
| VS Code | Integrated Development Environment (IDE) |
| MySQL Workbench | Database creation, administration, and query management |
| Git | Version control |
| GitHub | Collaborative development and source code management |

### Frameworks

The initial implementation will use Core Java and JDBC without an additional application framework. This keeps the project focused on demonstrating fundamental Java and OOP concepts.

### Application Interface

The application will use a **console-based (CLI) interface**.

Users will interact with the system through text-based menus, selecting operations and providing the required information through the terminal.

### Proposed Project Structure

```text
RehabilitationManagementSystem/
│
├── src/
│   ├── main/
│   │   └── Main.java
│   │
│   ├── models/
│   ├── dao/
│   ├── services/
│   ├── menus/
│   └── database/
│       └── DatabaseConnection.java
│
├── database/
│   └── schema.sql
│
├── README.md
└── .gitignore
```

The structure may be refined during implementation as the project develops.


## 5. Team Structure and Work Division

| Team | Members | Assigned Modules / Responsibilities |
|---|---|---|
| Team 1 | Member 1 & Member 2 | Patient Management and Staff Management |
| Team 2 | Member 3 & Member 4 | Medical Assessment and Laboratory Test Management |
| Team 3 | Member 5 & Member 6 | Vitals Management and Therapy Session Management |
| Team 4 | Member 7 & Member 8 | Medication Management and Discharge & Aftercare |
| Team 5 | Member 9 & Member 10 | Database Setup, JDBC Connectivity, Main Menu, System Integration and Testing |

### Team Responsibilities

**Team 1: Patient and Staff Management**
- Develop Patient and Staff-related model classes.
- Implement patient and staff management operations.
- Establish the foundational Person class and inheritance structure.

**Team 2: Medical Assessment and Laboratory Tests**
- Develop assessment and laboratory test classes.
- Implement assessment and test record management.
- Establish relationships with existing patient and staff records.

**Team 3: Vitals and Therapy**
- Develop Vitals and TherapySession classes.
- Implement recording and retrieval operations.
- Implement calculations such as total therapy time.

**Team 4: Medication and Aftercare**
- Develop Medication and AftercarePlan classes.
- Implement medication record management.
- Implement discharge planning and follow-up functionality.

**Team 5: Database, Integration and Testing**
- Establish the MySQL database and database connection.
- Implement the reusable JDBC connection component.
- Develop the main application menu and navigation.
- Coordinate the integration of all modules.
- Assist with system-wide validation, testing, and debugging.

All members are expected to participate in project planning, code reviews, integration, testing, and documentation.

---

**Project:** Rehabilitation Centre Management System (RCMS)  
**Programming Language:** Java  
**Database:** MySQL  
**Connectivity:** JDBC  
**Application Type:** Console-Based  
**Course Focus:** Object-Oriented Programming (OOP)