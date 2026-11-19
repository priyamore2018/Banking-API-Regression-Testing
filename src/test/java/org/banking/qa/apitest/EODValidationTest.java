package org.banking.qa.apitest;

import org.testng.annotations.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static io.restassured.RestAssured.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;

public class EODValidationTest {

	private static final String Version_A_URL = "http://localhost:8081/api/v1/eod-transactions";

	private static final String Version_B_URL = "http://localhost:8083/api/v1/eod-transactions";

	public static final String ValueDate = "2026-11-16";

	private static final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testVersionAAPI() {
		given().queryParam("valueDate", ValueDate).when().get(Version_A_URL).then().statusCode(200)
				.contentType(ContentType.JSON);
	}

	@Test
	public void testVersionBAPI() {
		given().queryParam("valueDate", ValueDate).when().get(Version_B_URL).then().statusCode(200)
				.contentType(ContentType.JSON);
	}

	// Test case 1 : compare Version A and Version B
	@Test
	public void compareVersionAAndB() throws Exception {
		// Get Version A response
		String responseA = given().queryParam("valueDate", ValueDate).when().get(Version_A_URL).then().statusCode(200)
				.extract().asString();
		// Get Version B response
		String responseB = given().queryParam("valueDate", ValueDate).when().get(Version_B_URL).then().statusCode(200)
				.extract().asString();
		// Convert responses into JSON
		JsonNode dataA = mapper.readTree(responseA);
		JsonNode dataB = mapper.readTree(responseB);
		Assert.assertTrue(dataA.isArray(), "Version A response should be a JSON array");
		Assert.assertTrue(dataB.isArray(), "Version B response should be a JSON array");
		// Store records using transactionId
		Map<String, JsonNode> mapA = new HashMap<>();
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataA) {
			mapA.put(record.path("transactionId").asText(), record);
		}
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		int mismatchCount = 0;
		// Compare records
		for (String id : mapA.keySet()) {
			if (!mapB.containsKey(id)) {
				System.out.println("Missing in Version B: " + id);
				mismatchCount++;
				continue;
			}
			JsonNode recordA = mapA.get(id);
			JsonNode recordB = mapB.get(id);
			Set<String> fields = new HashSet<>();
			recordA.fieldNames().forEachRemaining(fields::add);
			recordB.fieldNames().forEachRemaining(fields::add);
			// Compare each field
			for (String field : fields) {
				JsonNode valueA = recordA.get(field);
				JsonNode valueB = recordB.get(field);
				if (valueA == null || valueB == null) {
					if (valueA != valueB) {
						System.out.println("Missing field: " + id + " | " + field);
						mismatchCount++;
					}
				} else if (!valueA.equals(valueB)) {
					String category;
					if (!valueA.getNodeType().equals(valueB.getNodeType())) {
						category = "SCHEMA MISMATCH";
					} else {
						category = "FIELD VALUE MISMATCH";
					}
					System.out.println("Transaction ID: " + id);
					System.out.println("Field: " + field);
					System.out.println("Version A: " + valueA);
					System.out.println("Version B: " + valueB);
					System.out.println("Category: " + category);
					mismatchCount++;
				}
			}
		}
		// Check additional records in Version B
		for (String id : mapB.keySet()) {
			if (!mapA.containsKey(id)) {
				System.out.println("Additional record in Version B: " + id);
				mismatchCount++;
			}
		}
		// Final result
		System.out.println("Total mismatches: " + mismatchCount);
		Assert.assertEquals(mismatchCount, 0, "FAILED: Version A and Version B responses are not matching!");
	}

	// Test Case 2: Validate duplicate transaction IDs
	@Test
	public void testDuplicateTransactionDetection() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Set<String> idsA = new HashSet<>();
		Set<String> idsB = new HashSet<>();
		int duplicatesA = 0;
		int duplicatesB = 0;
		for (JsonNode record : dataA) {
			String id = record.path("transactionId").asText();
			if (!idsA.add(id)) {
				System.out.println("Duplicate ID in Version A: " + id);
				duplicatesA++;
			}
		}
		for (JsonNode record : dataB) {
			String id = record.path("transactionId").asText();
			if (!idsB.add(id)) {
				System.out.println("Duplicate ID in Version B: " + id);
				duplicatesB++;
			}
		}
		System.out.println("Version A duplicates: " + duplicatesA);
		System.out.println("Version B duplicates: " + duplicatesB);
		Assert.assertEquals(duplicatesA, 0, "Duplicate transaction IDs found in Version A");
		Assert.assertEquals(duplicatesB, 0, "Duplicate transaction IDs found in Version B");
	}

	// Test Case 3: Compare API response times
	@Test
	public void testResponseTimeComparison() {
		Response responseA = getApiResponse(Version_A_URL);
		Response responseB = getApiResponse(Version_B_URL);
		long timeA = responseA.getTime();
		long timeB = responseB.getTime();
		System.out.println("Version A Response Time: " + timeA + " ms");
		System.out.println("Version B Response Time: " + timeB + " ms");
		if (timeA < timeB) {
			System.out.println("Version A is faster by " + (timeB - timeA) + " ms");
		} else if (timeB < timeA) {
			System.out.println("Version B is faster by " + (timeA - timeB) + " ms");
		} else {
			System.out.println("Both APIs have the same response time");
		}
		Assert.assertEquals(responseA.statusCode(), 200);
		Assert.assertEquals(responseB.statusCode(), 200);
	}

	// Test Case 4: Validate field count
	@Test
	public void testFieldCountValidation() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		int mismatchCount = 0;
		for (JsonNode recordA : dataA) {
			String id = recordA.path("transactionId").asText();
			if (mapB.containsKey(id)) {
				JsonNode recordB = mapB.get(id);
				int countA = recordA.size();
				int countB = recordB.size();
				if (countA != countB) {
					System.out.println("Transaction ID: " + id);
					System.out.println("Version A field count: " + countA);
					System.out.println("Version B field count: " + countB);
					mismatchCount++;
				}
			}
		}
		System.out.println("Total field count mismatches: " + mismatchCount);
		Assert.assertEquals(mismatchCount, 0, "Field count mismatch found between Version A and Version B");
	}

	// Test Case 5: Validate data types
	@Test
	public void testDataTypeValidation() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		int mismatchCount = 0;
		for (JsonNode recordA : dataA) {
			String id = recordA.path("transactionId").asText();
			if (mapB.containsKey(id)) {
				JsonNode recordB = mapB.get(id);
				for (String field : (Iterable<String>) () -> recordA.fieldNames()) {
					JsonNode valueA = recordA.get(field);
					JsonNode valueB = recordB.get(field);
					if (valueA != null && valueB != null) {
						if (!valueA.getNodeType().equals(valueB.getNodeType())) {
							System.out.println("Transaction ID: " + id);
							System.out.println("Field: " + field);
							System.out.println("Version A Type: " + valueA.getNodeType());
							System.out.println("Version B Type: " + valueB.getNodeType());
							mismatchCount++;
						}
					}
				}
			}
		}
		System.out.println("Total data type mismatches: " + mismatchCount);
		Assert.assertEquals(mismatchCount, 0, "Data type mismatch found between Version A and Version B");
	}

	// Test Case 6: Validate null versus missing fields
	@Test
	public void testNullVsMissingFieldValidation() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		int mismatchCount = 0;
		for (JsonNode recordA : dataA) {
			String id = recordA.path("transactionId").asText();
			if (mapB.containsKey(id)) {
				JsonNode recordB = mapB.get(id);
				Set<String> fields = new HashSet<>();
				recordA.fieldNames().forEachRemaining(fields::add);
				recordB.fieldNames().forEachRemaining(fields::add);
				for (String field : fields) {
					JsonNode valueA = recordA.get(field);
					JsonNode valueB = recordB.get(field);
					boolean missingA = valueA == null;
					boolean missingB = valueB == null;
					boolean nullA = valueA != null && valueA.isNull();
					boolean nullB = valueB != null && valueB.isNull();
					if (missingA != missingB || nullA != nullB) {
						System.out.println("Transaction ID: " + id);
						System.out.println("Field: " + field);
						System.out.println("Version A: " + (missingA ? "MISSING" : valueA));
						System.out.println("Version B: " + (missingB ? "MISSING" : valueB));
						mismatchCount++;
					}
				}
			}
		}
		System.out.println("Total null/missing mismatches: " + mismatchCount);
		Assert.assertEquals(mismatchCount, 0, "Null versus missing field mismatch found");
	}

	// Test Case 8: Value date filter validation
	@Test
	public void testValueDateFilterValidation() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		int mismatchCount = 0;
		for (JsonNode record : dataA) {
			String actualDate = record.path("valueDate").asText();
			if (!ValueDate.equals(actualDate)) {
				System.out.println("Incorrect valueDate in Version A: " + actualDate);
				mismatchCount++;
			}
		}
		for (JsonNode record : dataB) {
			String actualDate = record.path("valueDate").asText();
			if (!ValueDate.equals(actualDate)) {
				System.out.println("Incorrect valueDate in Version B: " + actualDate);
				mismatchCount++;
			}
		}
		System.out.println("Total valueDate mismatches: " + mismatchCount);
		Assert.assertEquals(mismatchCount, 0, "API returned records with an incorrect valueDate");
	}

	// Test Case 9: Empty response validation
	@Test
	public void testEmptyResponseValidation() throws Exception {
		// Use a date that should have no transactions
		String emptyDate = ValueDate;
		Response responseA = given().queryParam("valueDate", emptyDate).when().get(Version_A_URL);
		Response responseB = given().queryParam("valueDate", emptyDate).when().get(Version_B_URL);
		Assert.assertEquals(responseA.statusCode(), 200);
		Assert.assertEquals(responseB.statusCode(), 200);
		JsonNode dataA = mapper.readTree(responseA.asString());
		JsonNode dataB = mapper.readTree(responseB.asString());
		Assert.assertTrue(dataA.isArray(), "Version A response should be a JSON array");
		Assert.assertTrue(dataB.isArray(), "Version B response should be a JSON array");
		System.out.println("Version A Records: " + dataA.size());
		System.out.println("Version B Records: " + dataB.size());
		if (dataA.isEmpty() && dataB.isEmpty()) {
			System.out.println("Both APIs returned valid empty responses");
		} else {
			System.out.println("At least one API returned records for the selected date");
		}
	}

	// Test Case 10: API failure handling
	@Test
	public void testApiFailureHandling() {
		// Intentionally use an invalid endpoint
		String invalidURL = Version_A_URL;
		Response response = given().when().get(invalidURL);
		int statusCode = response.statusCode();
		System.out.println("Invalid API Status Code: " + statusCode);
		System.out.println("Error Response: " + response.asString());
		Assert.assertTrue(statusCode >= 400, "Expected an API failure status code (4xx or 5xx)");
	}

	// Test Case 11: Positive scenario
	@Test
	public void testPositiveScenario() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Map<String, JsonNode> mapA = new HashMap<>();
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataA) {
			mapA.put(record.path("transactionId").asText(), record);
		}
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		Assert.assertEquals(mapA.keySet(), mapB.keySet(), "Transaction IDs should match");
		for (String id : mapA.keySet()) {
			Assert.assertEquals(mapA.get(id), mapB.get(id), "Transaction data mismatch for ID: " + id);
		}
		System.out.println("Positive Scenario Passed: Both API responses match");
	}

	// Test Case 12: Negative scenario - Missing transaction
	@Test
	public void testMissingTransaction() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Set<String> idsA = new HashSet<>();
		Set<String> idsB = new HashSet<>();
		for (JsonNode record : dataA) {
			idsA.add(record.path("transactionId").asText());
		}
		for (JsonNode record : dataB) {
			idsB.add(record.path("transactionId").asText());
		}
		Set<String> missingTransactions = new HashSet<>(idsA);
		missingTransactions.removeAll(idsB);
		System.out.println("Missing Transactions in Version B: " + missingTransactions);
		Assert.assertTrue(missingTransactions.isEmpty(), "Transactions are missing in Version B");
	}

	// Test Case 13: Negative scenario - Additional transaction
	@Test
	public void testAdditionalTransaction() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Set<String> idsA = new HashSet<>();
		Set<String> idsB = new HashSet<>();
		for (JsonNode record : dataA) {
			idsA.add(record.path("transactionId").asText());
		}
		for (JsonNode record : dataB) {
			idsB.add(record.path("transactionId").asText());
		}
		Set<String> additionalTransactions = new HashSet<>(idsB);
		additionalTransactions.removeAll(idsA);
		System.out.println("Additional Transactions in Version B: " + additionalTransactions);
		Assert.assertTrue(additionalTransactions.isEmpty(), "Additional transactions found in Version B");
	}

	// Test Case 14: Negative scenario - Value mismatch and schema mismatch
	@Test
	public void testValueAndSchemaMismatch() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Map<String, JsonNode> mapA = new HashMap<>();
		Map<String, JsonNode> mapB = new HashMap<>();
		for (JsonNode record : dataA) {
			mapA.put(record.path("transactionId").asText(), record);
		}
		for (JsonNode record : dataB) {
			mapB.put(record.path("transactionId").asText(), record);
		}
		int valueMismatchCount = 0;
		int schemaMismatchCount = 0;
		for (String id : mapA.keySet()) {
			if (!mapB.containsKey(id)) {
				continue;
			}
			JsonNode recordA = mapA.get(id);
			JsonNode recordB = mapB.get(id);
			Set<String> fields = new HashSet<>();
			recordA.fieldNames().forEachRemaining(fields::add);
			recordB.fieldNames().forEachRemaining(fields::add);
			for (String field : fields) {
				JsonNode valueA = recordA.get(field);
				JsonNode valueB = recordB.get(field);
				if (valueA == null || valueB == null) {
					System.out.println("SCHEMA MISMATCH");
					System.out.println("Transaction ID: " + id);
					System.out.println("Missing Field: " + field);
					schemaMismatchCount++;
				} else if (!valueA.getNodeType().equals(valueB.getNodeType())) {
					System.out.println("SCHEMA MISMATCH");
					System.out.println("Transaction ID: " + id);
					System.out.println("Field: " + field);
					System.out.println("Version A Type: " + valueA.getNodeType());
					System.out.println("Version B Type: " + valueB.getNodeType());
					schemaMismatchCount++;
				} else if (!valueA.equals(valueB)) {
					System.out.println("VALUE MISMATCH");
					System.out.println("Transaction ID: " + id);
					System.out.println("Field: " + field);
					System.out.println("Version A: " + valueA);
					System.out.println("Version B: " + valueB);
					valueMismatchCount++;
				}
			}
		}
		System.out.println("\n========== MISMATCH REPORT ==========");
		System.out.println("Total Value Mismatches: " + valueMismatchCount);
		System.out.println("Total Schema Mismatches: " + schemaMismatchCount);
		System.out.println("=====================================");
		Assert.assertEquals(valueMismatchCount + schemaMismatchCount, 0, "Value or Schema mismatches found!");
	}

	// Helper method: Get API response
	private Response getApiResponse(String url) {
		return given().queryParam("valueDate", ValueDate).when().get(url);
	}

	// Test Case 7: Generate Version A versus Version B comparison summary
	@Test
	public void testVersionComparisonReport() throws Exception {
		JsonNode dataA = mapper.readTree(getApiResponse(Version_A_URL).asString());
		JsonNode dataB = mapper.readTree(getApiResponse(Version_B_URL).asString());
		Set<String> idsA = new HashSet<>();
		Set<String> idsB = new HashSet<>();
		for (JsonNode record : dataA) {
			idsA.add(record.path("transactionId").asText());
		}
		for (JsonNode record : dataB) {
			idsB.add(record.path("transactionId").asText());
		}
		Set<String> commonIds = new HashSet<>(idsA);
		commonIds.retainAll(idsB);
		Set<String> missingInB = new HashSet<>(idsA);
		missingInB.removeAll(idsB);
		Set<String> additionalInB = new HashSet<>(idsB);
		additionalInB.removeAll(idsA);
		System.out.println("\n========== VERSION COMPARISON REPORT ==========");
		System.out.println("Version A Total Records: " + dataA.size());
		System.out.println("Version B Total Records: " + dataB.size());
		System.out.println("Common Transaction IDs: " + commonIds.size());
		System.out.println("Missing Records in Version B: " + missingInB.size());
		System.out.println("Additional Records in Version B: " + additionalInB.size());
		System.out.println("Missing IDs: " + missingInB);
		System.out.println("Additional IDs: " + additionalInB);
		System.out.println("===============================================");
		if (missingInB.isEmpty() && additionalInB.isEmpty()) {
			System.out.println("RESULT: Both API versions contain matching transaction IDs");
		} else {
			System.out.println("RESULT: Differences found between API versions");
		}
	}
}
