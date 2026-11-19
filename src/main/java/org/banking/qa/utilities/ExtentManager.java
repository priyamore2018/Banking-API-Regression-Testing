package org.banking.qa.utilities;

import java.time.LocalDateTime;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class ExtentManager {
private static ExtentReports report;
	
	public static ExtentReports getReport() {
		
		ExtentSparkReporter spark= new ExtentSparkReporter("/Users/hrushi/Desktop/Priyanka/eclipse_workspace/Banking-API-Regression-Testing/ExtentReport");
		
		report=new ExtentReports();
		report.attachReporter(spark);
		
		report.setSystemInfo("Project", "EOD API Comparison");
		report.setSystemInfo("Framework", "Rest Assured + TestNG");
		report.setSystemInfo("Execution Date", LocalDateTime.now().toString());
		//report.setSystemInfo("Value Date", VALUE_DATE);
    
		return report;
}
}
