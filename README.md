# Automated Excel Data Model Tool

A robust Java application that automates the transformation of unstructured Excel spreadsheets into normalized SQL database schemas, leveraging heuristic algorithms and relational database theory.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)


---

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [Usage Example](#usage-example)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)
- [Author](#author)

---

## 🎯 About

The **Automated Excel Data Model Tool** addresses the challenge of migrating legacy data from unstructured spreadsheets into well-designed relational databases. Developed as part of a Bachelor's Thesis, this application applies database normalization theory (1NF, 2NF) and intelligent heuristics to:

- **Parse complex Excel files** with messy, multi-valued cells
- **Detect data patterns** such as currencies, units of measurement, and composite values
- **Automatically normalize** data to eliminate redundancy and ensure integrity
- **Generate SQL scripts** with proper constraints (Primary Keys, Foreign Keys)

This tool drastically reduces manual effort in data modeling and ensures consistency across database migrations.

---

## ✨ Features

### Core Capabilities
- **Excel Parsing**: Reads `.xls` and `.xlsx` files using Apache POI
- **Automated First Normal Form (1NF)**:
  - Splits multi-valued cells (e.g., `"Red, Blue"` → separate rows)
  - Decomposes composite attributes using heuristic rules
- **Automated Second Normal Form (2NF)**:
  - Identifies candidate keys and partial dependencies
  - Decomposes relations to eliminate redundancy
- **Pattern Recognition Heuristics**:
  - **Currency**: `"$50.00"` → `Amount: 50.00, Currency: USD`
  - **Quantity-Item**: `"5 Books"` → `Quantity: 5, Item: Books`
  - **Value-Unit**: `"20 kg"` → `Value: 20, Unit: kg`
  - **Parenthetical Aliases**: `"Google (Alphabet)"` → `Primary: Google, Alias: Alphabet`
- **SQL Script Generation**: Creates `CREATE TABLE` and `INSERT` statements with proper data types and constraints
- **SOLID Design Principles**: Modular, maintainable, and extensible architecture

---

## 🏗️ Architecture

The application follows a layered architecture:

```
┌─────────────────────────────────────────┐
│          Main Entry Point               │
│       (User Input & Workflow)           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         I/O Layer                       │
│  • ExcelFileReader                      │
│  • SqlGenerator                         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      Normalization Layer                │
│  • FirstNormalizer (1NF)                │
│  • SecondNormalizer (2NF)               │
│  • CandidateKeyIdentifier               │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      Heuristic Rules Engine             │
│  • CurrencyHeuristic                    │
│  • QuantityItemHeuristic                │
│  • ValueUnitHeuristic                   │
│  • ParentheticalAliasHeuristic          │
└─────────────────────────────────────────┘
```

---

## 🔧 Prerequisites

Before running this project, ensure you have the following installed:

### Required Software

| Software | Version | Download Link |
|----------|---------|---------------|
| **Java JDK** | 21 or higher | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://jdk.java.net/) |
| **Apache Maven** | 3.6+ | [Maven](https://maven.apache.org/download.cgi) |
| **Git** | Latest | [Git](https://git-scm.com/downloads) |

### Optional (Recommended)
- **IntelliJ IDEA** (Community or Ultimate Edition) for IDE support
- **VS Code** with Java extensions
- **Antigravity** 

### Verify Installation

```bash
# Check Java version
java -version
# Expected output: java version "21.x.x" or higher

# Check Maven version
mvn -version
# Expected output: Apache Maven 3.x.x

# Check Git version
git --version
```

---

## 📦 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/melisa-cihan/Automated-Excel-DataModel-Tool.git

cd Automated-Excel-DataModel-Tool
```

### 2. Install Maven Dependencies

There are **two ways** to install dependencies:

#### Option A: Using Command Line 

Navigate to the project root directory (where `pom.xml` is located) and run:

```bash
# From: /Users/melisacihan/Desktop/Automated-Excel-DataModel-Tool/
mvn clean install
```

**What this does:**
- `clean`: Removes previous build artifacts from the `target/` directory
- `install`: Downloads all dependencies defined in `pom.xml` and installs them to your local Maven repository

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

#### Option B: Using IntelliJ IDEA

1. **Open the project** in IntelliJ IDEA
2. IntelliJ will **automatically detect** the `pom.xml` file
3. Wait for the IDE to **auto-import dependencies** (look for progress in the bottom-right corner)
4. **Manual trigger** (if auto-import doesn't start):
   - Right-click on `pom.xml`
   - Select **"Maven"** → **"Reload Project"**
   - Or click the **Maven Tool Window** (right sidebar) → **Reload All Maven Projects** button (↻)

**Verify dependencies are loaded:**
- Open the **Maven Tool Window** (View → Tool Windows → Maven)
- Expand **Dependencies** to see:
  - Apache POI (5.4.1)
  - Log4j (2.25.3)
  - JUnit Jupiter (5.10.1)

---

## 🚀 Running the Application

### Option A: Using Command Line

From the project root directory:

```bash
# Compile the project
mvn compile

# Run the main class
mvn exec:java -Dexec.mainClass="org.melisa.datamodel.Main"
```

**Alternative (after building JAR):**
```bash
# Build the JAR file
mvn package

# Run the JAR
java -jar target/Automated-Excel-DataModel-Tool-1.0-SNAPSHOT.jar
```

### Option B: Using IntelliJ IDEA

#### Method 1: Run from Main Class
1. Navigate to `src/main/java/org/melisa/datamodel/Main.java`
2. Right-click on the file or the `main` method
3. Select **"Run 'Main.main()'"**
4. Or use the **green play button** (▶) in the gutter next to `public static void main`

#### Method 2: Create a Run Configuration
1. Go to **Run** → **Edit Configurations...**
2. Click **+** → **Application**
3. Set:
   - **Name**: `Automated Excel Tool`
   - **Main class**: `org.melisa.datamodel.Main`
   - **Working directory**: Project root
4. Click **OK**
5. Select the configuration from the dropdown and click **Run** (▶)

---

## 📖 Usage Example

### Interactive Workflow

Once you run the application, you'll be prompted for input:

```
Please enter the full path to your Excel file (e.g., C:\data\mydata.xlsx or /home/user/data.xls):
> /Users/username/Documents/sample_data.xlsx

Please enter the desired SQL table name base (e.g., 'Order').
If left blank, a default name like 'EXCEL_DATA' will be used:
> Customer

--- Step 1: Reading Excel data ---
Excel data read successfully. Number of rows detected: 45

--- Step 2: Normalizing data to First Normal Form (1NF) ---
1NF Normalization complete. Number of normalized rows: 52

--- Step 3: Decomposing data to Second Normal Form (2NF) ---
Selected Candidate Key for 2NF: [CustomerID, OrderID]
Decomposed Relation: CUSTOMER_DETAILS created with attributes: [CustomerName, Email]
2NF Decomposition complete. Generated 2 new relation(s).

--- Step 4: Displaying 2NF Relations and Generating SQL ---

== Relation Name: CUSTOMER_DETAILS ==
   Primary Keys: [CUSTOMERID]
   Foreign Keys: {}

--- START SQL SCRIPT for CUSTOMER_DETAILS ---

CREATE TABLE CUSTOMER_DETAILS (
    CUSTOMERID INTEGER NOT NULL,
    CUSTOMERNAME VARCHAR(255),
    EMAIL VARCHAR(255),
    CONSTRAINT PK_CUSTOMER_DETAILS PRIMARY KEY (CUSTOMERID)
);

INSERT INTO CUSTOMER_DETAILS (CUSTOMERID, CUSTOMERNAME, EMAIL)
VALUES (101, 'John Doe', 'john@example.com');
...

--- END SQL SCRIPT for CUSTOMER_DETAILS ---
```

### Sample Input Excel Structure

| CustomerID | OrderID | Product     | Price  |
|------------|---------|-------------|--------|
| 1          | 101     | Laptop      | 50 €   |
| 1          | 102     | Mouse       | 25 €   |
| 2          | 103     | Keyboard    | 30 €   |

### Generated Output (2NF Relations)

**Table 1: CUSTOMER_DETAILS**
- Primary Key: `CUSTOMERID`
- Columns: `CUSTOMERID`, `CUSTOMERNAME`, `EMAIL`

**Table 2: ORDER_MAIN**
- Primary Key: `CUSTOMERID, ORDERID`
- Foreign Key: `CUSTOMERID → CUSTOMER_DETAILS(CUSTOMERID)`
- Columns: `CUSTOMERID`, `ORDERID`, `PRODUCT_NAME`, `PRICE_AMOUNT`, `PRICE_CURRENCY`

---

## 📁 Project Structure

```
Automated-Excel-DataModel-Tool/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── org/melisa/datamodel/
│   │           ├── Main.java                        # Entry point
│   │           ├── io/
│   │           │   ├── ExcelFileReader.java         # Excel parsing
│   │           │   └── SqlGenerator.java            # SQL script generation
│   │           ├── model/
│   │           │   └── DecomposedRelation.java      # Data model for 2NF relations
│   │           └── normalization/
│   │               ├── FirstNormalizer.java         # 1NF transformation
│   │               ├── SecondNormalizer.java        # 2NF decomposition
│   │               ├── CandidateKeyIdentifier.java  # Key detection
│   │               └── heuristics/
│   │                   ├── HeuristicRule.java       # Interface for heuristics
│   │                   ├── CurrencyHeuristic.java
│   │                   ├── QuantityItemHeuristic.java
│   │                   ├── ValueUnitHeuristic.java
│   │                   └── ParentheticalAliasHeuristic.java
│   │
│   └── test/
│       └── java/
│           └── org/melisa/datamodel/
│               ├── io/
│               │   ├── ExcelFileReaderTest.java
│               │   └── SqlGeneratorTest.java
│               └── normalization/
│                   ├── FirstNormalizerTest.java
│                   ├── SecondNormalizerTest.java
│                   └── CandidateKeyIdentifierTest.java
│
├── pom.xml                # Maven configuration and dependencies
├── README.md              # This file
└── .gitignore             # Git ignore rules
```

---

## 🧪 Testing

The project includes comprehensive unit tests using **JUnit 5**.

### Run All Tests

#### Via Command Line
```bash
# From project root: /Users/melisacihan/Desktop/Automated-Excel-DataModel-Tool/
mvn test
```

#### Via IntelliJ IDEA
1. Right-click on `src/test/java` directory
2. Select **"Run 'All Tests'"**
3. Or use the **Maven Tool Window** → **Lifecycle** → Double-click **test**

### Test Coverage

- **I/O Layer**: Tests for Excel reading and SQL generation
- **Normalization**: Tests for 1NF and 2NF transformations
- **Heuristics**: Tests for pattern recognition accuracy
- **Edge Cases**: Null handling, duplicate column names, empty datasets


---

## 👤 Author

**Melisa Cihan**

- GitHub: [@melisa-cihan](https://github.com/melisa-cihan)
- Project Repository: [Automated-Excel-DataModel-Tool](https://github.com/melisa-cihan/Automated-Excel-DataModel-Tool)

---

## 📚 Additional Resources

- [Apache POI Documentation](https://poi.apache.org/apidocs/dev/)
- [Database Normalization Guide](https://en.wikipedia.org/wiki/Database_normalization)
- [Maven Getting Started](https://maven.apache.org/guides/getting-started/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

---


