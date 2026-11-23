# Banking API Regression Testing

This project checks that the **new API (Version B)** returns the same end-of-day (EOD) banking transaction data as the **old API (Version A)** for the same date.

It has three parts:

| Part | What it is | Port |
|------|------------|------|
| Version A | Spring Boot app (old system, baseline). Built with Maven. | 8081 |
| Version B | Spring Boot app (new system, candidate). Built with Gradle. | 8083 |
| Test project | Java tests that call both APIs and compare the JSON replies | - |

All data is **synthetic test data**. No real customer or account information is used.

---

## 1. Architecture

```
                 +-------------------------------+
                 |   Banking-API-Regression-     |
                 |   Testing (this project)      |
                 |  TestNG + RestAssured + Jackson|
                 +---------------+---------------+
                                 |
            GET /api/v1/eod-transactions?valueDate=YYYY-MM-DD
                 +---------------+---------------+
                 |                               |
        +--------v--------+             +--------v--------+
        |   Version A     |             |   Version B     |
        |   :8081         |             |   :8083         |
        | Controller      |             | Controller      |
        | Service         |             | Service         |
        | Repository      |             | Repository      |
        | Data store      |             | Data store      |
        +-----------------+             +-----------------+
                                 |
                 Reports: ExtentReport/Index.html, logs/, test-output/
```

Flow: load the same data into A and B, call both APIs for one value date, compare the replies, write reports.

---
## 2. Tools and versions

| Tool | Version | Use |
|------|---------|-----|
| Java | 23 (Version A, tests), 21 (Version B) | Language |
| Spring Boot | see each project | The two services |
| Maven | 3.x | Builds the test project and Version A |
| Gradle | wrapper (`gradlew`) | Builds Version B |
| TestNG | 7.x | Runs the tests (`testng.xml`) |
| RestAssured | 5.5.0 | Calls the APIs |
| Jackson Databind | 2.18.2 | Reads and compares JSON |
| ExtentReports | 5.1.2 | HTML report |
| Log4j | 2.22.1 | Logs |

## 3. Project structure

### Test project: `Banking-API-Regression-Testing`

```
src/main/java
  org.banking.qa.aipdataloader/TransactionLoader.java   loads test data
  org.banking.qa.listener/Listener.java                 TestNG listener for reports
  org.banking.qa.utilities/ExtentManager.java           builds the Extent report
  org.banking.qa.utilities/LogUtil.java                 logging helper
src/main/resources
  transactiondata.json                                  synthetic test data
src/test/java
  org.banking.qa.apitest/EODValidationTest.java         all test cases
ExtentReport/Index.html                                 HTML report (generated)
logs/                                                   log files (generated)
test-output/                                            TestNG output (generated)
pom.xml   testng.xml   README.md
```

### Version A: `StringBootVersionAApplication` (Maven)

```
org.yaazhtech.versiona            Transaction, VersionAApplication
org.yaazhtech.versiona.repository TransactionRepository
org.yaazhtech.versiona.service    TransactionService
org.yaazhtech.versiona.web        TransactionController
src/main/resources/application.properties
```

### Version B: `StringBootProjectDemo2` (Gradle)

```
com.example.springbootprojectdemo2.controller   TransactionController
com.example.springbootprojectdemo2.repository   TransactionRepository
com.example.springbootprojectdemo2.service      TransactionService
com.example.StringBootProjectDemo2              Application, Transaction
src/main/resources/application.properties
```

---

## 4. Prerequisites

- JDK 23 (JDK 21 is enough for Version B)
- Maven 3.x
- Internet access the first time, to download dependencies
- Free ports **8081** and **8083**

Check your setup:

```bash
java -version
mvn -version
```

---

## 5. API details

Both services use the same endpoint.

```
GET /api/v1/eod-transactions?valueDate=2026-11-16
```

| Item | Value |
|------|-------|
| Version A URL | `http://localhost:8081/api/v1/eod-transactions` |
| Version B URL | `http://localhost:8083/api/v1/eod-transactions` |
| Query parameter | `valueDate` (format `YYYY-MM-DD`) |
| Success reply | HTTP 200, JSON list of transactions |
| No data for the date | HTTP 200, empty list `[]` |
| Bad or missing input | HTTP 4xx |

### Transaction fields (10 per record)

| Field | Type | Notes |
|-------|------|-------|
| transactionId | String | Unique key used to match A and B |
| accountId | String | Synthetic account, for example `A123` |
| valueDate | String (date) | Used to select the EOD records |
| bookingDate | String (date) | Booking date |
| currency | String | EUR, USD, GBP |
| amount | Number | Example `250.00` |
| debitCreditIndicator | String | `DEBIT` or `CREDIT` |
| transactionType | String | PAYMENT, TRANSFER, REFUND, FEE |
| description | String or null | Can be null |
| status | String | BOOKED or PENDING |

---

## 6. Configuration

Right now these values are written inside `EODValidationTest.java`:

| Setting | Current value |
|---------|---------------|
| `Version_A_URL` | `http://localhost:8081/api/v1/eod-transactions` |
| `Version_B_URL` | `http://localhost:8083/api/v1/eod-transactions` |
| `ValueDate` | `2026-11-16` |
| Empty-date test | `2026-11-18` |

To change the ports of the services, edit `server.port` in each project's `src/main/resources/application.properties`.

> Improvement idea: move URLs and dates to a config file or `-D` system properties.

---

## 7. How to run

### Step 1: Start Version A (port 8081)

```bash
cd StringBootVersionAApplication
mvn spring-boot:run
```

### Step 2: Start Version B (port 8083)

```bash
cd StringBootProjectDemo2
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

### Step 3: Load the test data

The synthetic dataset is in `src/main/resources/transactiondata.json` in the test project. Load the same data into both services (with `TransactionLoader` or the seed method you use in each app).

Check that data is loaded:

```bash
curl "http://localhost:8081/api/v1/eod-transactions?valueDate=2026-11-16"
curl "http://localhost:8083/api/v1/eod-transactions?valueDate=2026-11-16"
```

### Step 4: Run the tests

```bash
cd Banking-API-Regression-Testing
mvn clean test
```

Maven runs the suite defined in `testng.xml`.

### Step 5: Open the reports

| Report | Location |
|--------|----------|
| Extent HTML report | `ExtentReport/Index.html` |
| Log files | `logs/` |
| TestNG results | `test-output/` |
| Mismatch details | Console output |

---

## 8. Test data

- 2 value dates: `2026-11-16` and `2026-11-17`
- Accounts `A123` to `A127`
- Currencies: EUR, USD, GBP
- Types: PAYMENT, TRANSFER, REFUND, FEE
- Status: BOOKED, PENDING
- Comparison key: `transactionId`

Sample data difference between the two versions on `2026-11-16`:

| Transaction | Field | Version A | Version B | Type of problem |
|-------------|-------|-----------|-----------|-----------------|
| T001 | whole record | not present | present | Extra record in B |
| T002 | currency | EUR | USD | Value mismatch |
| T002 | description | "Payment transaction" | null | Schema (type) mismatch |

On `2026-11-17` (T005 to T009) both versions are identical. This is the positive case.

---

## 9. Comparison rules

| Rule | How it works |
|------|--------------|
| Match key | Records are matched by `transactionId`. Order does not matter. |
| Field set | All field names from A and B are checked, so missing or extra fields are found. |
| Value equality | Values must match exactly. Text is case-sensitive and spaces are not removed. Numbers are compared as numbers, so `250.00` equals `250.0`. |
| Schema or value | If the data type is different, it is a **SCHEMA MISMATCH**. If the type is the same but the value is different, it is a **FIELD VALUE MISMATCH**. |
| Null vs missing | A missing field, a null, and a real value are three different things. Any difference is reported. |
| Duplicates | The same `transactionId` twice in one reply is reported as a duplicate. |
| Value date filter | Every record must have the requested `valueDate`. |

---

## 10. Test cases

| # | Test method | What it checks | Expected on 16 Nov data |
|---|-------------|----------------|-------------------------|
| S1-S2 | `testVersionAAPI`, `testVersionBAPI` | HTTP 200 and JSON format | PASS |
| TC1 | `compareVersionAAndB` | Full comparison by transactionId | FAIL (3 differences) |
| TC2 | `testDuplicateTransactionDetection` | Repeated transactionId | PASS |
| TC3 | `testResponseTimeComparison` | Response time and status 200 | PASS (informational) |
| TC4 | `testFieldCountValidation` | Number of fields per record | PASS |
| TC5 | `testDataTypeValidation` | Data type of each field | FAIL (T002 description) |
| TC6 | `testNullVsMissingFieldValidation` | Null, missing or value | FAIL (T002 description) |
| TC8 | `testValueDateFilterValidation` | Only the requested date is returned | PASS |
| TC9 | `testEmptyResponseValidation` | Empty reply for 2026-11-18 | PASS |
| TC10 | `testApiFailureHandling` | Error status when `valueDate` is missing | PASS if API returns 4xx |
| TC11 | `testPositiveScenario` | A and B are fully equal | FAIL now, PASS when A equals B |
| TC12 | `testMissingTransaction` | Records missing in B | PASS (none missing) |
| TC13 | `testAdditionalTransaction` | Extra records in B | FAIL (T001) |
| TC14 | `testValueAndSchemaMismatch` | Value and schema differences counted separately | FAIL (1 value, 1 schema) |
| RPT | `testVersionComparisonReport` | Summary of totals and IDs (no pass/fail) | Information only |

> The expected results come from analysing the sample JSON. Please confirm them with your own run.

---

## 11. How to read the results

- **PASS**: the check found no problem.
- **FAIL**: the check found a difference. The console shows the details.
- A mismatch shows:
  - `Transaction ID`
  - `Field`
  - `Version A` value
  - `Version B` value
  - `Category` (`SCHEMA MISMATCH` or `FIELD VALUE MISMATCH`)
- `Missing in Version B: <id>` means the record is in A but not in B.
- `Additional record in Version B: <id>` means the record is only in B.
- The line `Total mismatches: N` is the total number of differences.

**Overall result:** If TC1 has `Total mismatches: 0`, Version B matches Version A. If not, Version B is **not ready**.

---

## 12. Assumptions

- Version A is the correct baseline.
- `transactionId` is unique and is the matching key.
- Both services return a JSON list (array) of transactions.
- Data is synthetic and the same data is loaded into both services.
- Services are running on `localhost` before the tests start.

---

## 13. Known limitations and next steps

- The load report for each service (input, inserted, rejected, duplicates, check count) is not part of `EODValidationTest`. Add it from `TransactionLoader`.
- There is no single combined A-vs-B report file with run ID and time. Add a JSON, CSV or HTML summary.
- The data set is small. Add many more records, negative amounts and rejected status.
- Renamed field, new field and "service stopped" cases are not shown yet.
- There are no mapping or exclusion rules for changes that are on purpose.
- URLs, ports and dates are written inside the code. Move them to a config file.
- Tests repeat some parsing code. Move it to one shared compare helper.
- `pom.xml` lists TestNG twice (7.10.2 and 7.11.0), Surefire twice, and has an unused JUnit 3.8.1. Clean it.
- There is no Test Case 7 in the numbering.
- Run the suite in CI/CD (Jenkins or GitHub Actions) and publish the Extent report.

