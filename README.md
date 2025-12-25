# Automated-Excel-DataModel-Tool
# Welcome to the Automated Excel Data Model Tool
A robust Java application that automates the transformation of unstructured Excel spreadsheets into normalized SQL database schemas.

## Table of Contents
0. [About](#about)
1. [Features](#features)
2. [Tech-Stack](#tech-stack)
3. [Getting Started](#getting-started)
4. [Usage](#usage)
5. [AI Usage](#ai-usage)
6. [Author](#author)

## About
The **Automated Excel Data Model Tool** is designed to solve the challenge of migrating legacy data from chaotic spreadsheets into structured relational databases. Developed as part of a Bachelor's Thesis, this tool utilizes heuristic algorithms to recognize patterns (such as currencies and physical units) and automatically applies database normalization rules (1NF, 2NF). It ensures data integrity and drastically reduces the manual effort required for data modelling.

## Features
- **Excel Parsing:** Efficiently reads `.xlsx` files using the Apache POI library.
- **Automated Normalization:**
    - **1NF (Atomicity):** Decomposes complex strings into atomic values.
    - **2NF (Relationships):** Eliminates partial dependencies by decomposing relations.
- **Heuristic Pattern Recognition:** Detects and handles specific data types:
    - Currency values (e.g., "$50.00")
    - Physical quantities (e.g., "10 kg")
    - Parenthetical aliases
    - Value Unit (e.g., "10 Apples")
- **SQL Generation:** Automatically generates ready-to-execute `.sql` scripts.
- **Component Design:** Built with adherence to SOLID principles.

## Tech Stack
- **Language:** Java 21
- **Build Tool:** Maven
- **Libraries:** Apache POI (Excel Processing)
- **Testing:** JUnit 5

## Getting Started

### Pre-Requisites
- Install [Java JDK 21](https://www.oracle.com/java/technologies/downloads/) or higher.
- Install [Maven](https://maven.apache.org/).
- A Java IDE (IntelliJ IDEA is recommended).
- Git

### Clone the Repository
```bash
git clone git@github.com:melisa-cihan/Automated-Excel-DataModel-Tool.git
```

## Author
**Melisa Cihan**