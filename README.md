# Banking API Regression Testing

Compares the end-of-day (EOD) transaction API of **Version A** (old, baseline) with **Version B** (new, candidate) and produces HTML/JSON reports. All data is synthetic.

## Projects

| Project | Role | Port |
|---|---|---|
| `StringBootProjectDemo` | Version A (Spring Boot, Maven) | 8081 |
| `StringBootProjectDemo2` | Version B (Spring Boot, Gradle) | 8083 |
| `Banking-API-Regression-Testing` | This test suite (TestNG, Jackson, Java HttpClient) | – |


## Prerequisites

JDK 17+, Maven 3.9+, ports 8081 and 8083 free.

## How to run

```bash
# 1. Start both apps
cd StringBootProjectDemo  && mvn spring-boot:run      # :8081
cd StringBootProjectDemo2 && ./gradlew bootRun        # :8083

# 2. (optional) Generate a new dataset: 1000 records, 3 value dates
cd Banking-API-Regression-Testing
mvn compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=org.banking.qa.loader.DatasetGenerator \
  -Dexec.args="1000 src/main/resources/transactiondata.json false"

# 3. Run everything (load -> compare -> offline scenarios)
mvn clean test
```

Change settings without editing code:
```bash
mvn clean test -DversionA.url=http://localhost:8081 -DversionB.url=http://localhost:8083 -DvalueDate=2026-11-16
```
All defaults are in `src/main/resources/config.properties`.

## What runs

| Suite | Class | What it does |
|---|---|---|
| Suite 1 | `LoadSuiteTest` | Loads the dataset into A and B, verifies counts by calling the EOD endpoint, writes a load report |
| Suite 2 | `EODValidationTest` | Compares A vs B live, checks value-date filter, empty result, invalid date |
| Scenarios | `ComparatorScenarioTest` | Offline proof that every defect type is detected (missing, extra, value, schema, null vs missing, duplicate, API failure) |

Run only the scenarios (no services needed except a closed port): `mvn test -Dtest=ComparatorScenarioTest`

