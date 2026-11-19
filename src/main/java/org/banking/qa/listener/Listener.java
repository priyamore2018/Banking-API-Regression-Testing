
package org.banking.qa.listener;

import org.apache.logging.log4j.Logger;
import org.banking.qa.utilities.ExtentManager;
import org.banking.qa.utilities.LogUtil;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

public class Listener implements ITestListener {

    private static ExtentReports reports =
            ExtentManager.getReport();

    public static ThreadLocal<ExtentTest> testThread =
            new ThreadLocal<>();

    Logger log = LogUtil.getLogger(Listener.class);

    @Override
    public void onStart(ITestContext context) {
        log.info("Test Suite Started: " + context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {

        String methodName =
                result.getMethod().getMethodName();

        ExtentTest test =
                reports.createTest(methodName);

        testThread.set(test);

        test.log(Status.INFO,
                "Test Case Started: " + methodName);

        log.info("Test Case Started: " + methodName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {

        getTest().log(Status.PASS,
                "Test Case Passed Successfully.");

        log.info("Test Case Passed: "
                + result.getMethod().getMethodName());

        testThread.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {

        getTest().log(Status.FAIL,
                "Test Case Failed.");

        getTest().log(Status.FAIL,
                result.getThrowable());

        log.error("Test Case Failed: "
                + result.getMethod().getMethodName());

        testThread.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {

        getTest().log(Status.SKIP,
                "Test Case Skipped.");

        log.warn("Test Case Skipped: "
                + result.getMethod().getMethodName());

        testThread.remove();
    }

    @Override
    public void onFinish(ITestContext context) {

        log.info("Test Suite Finished: " + context.getName());

        reports.flush();
    }

    public ExtentTest getTest() {
        return testThread.get();
    }
}
