import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject

import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile

import internal.GlobalVariable as GlobalVariable

import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext

class CurrentTestIdListener {
	
	private static List<TestSuiteContext> tsContexts = Collections.synchronizedList(new ArrayList<TestSuiteContext>())
	private static List<TestCaseContext> tcContexts = Collections.synchronizedList(new ArrayList<TestCaseContext>())
	
	@BeforeTestSuite
	void beforeTestSuite(TestSuiteContext testSuiteContext) {
		tsContexts.add(testSuiteContext)
		println("BeforeTestSuite: ${testSuiteContext.getTestSuiteId()}")
	}
	
	@BeforeTestCase
	void beforeTestCase(TestCaseContext testCaseContext) {
		tcContexts.add(testCaseContext)
		println("BeforeTestCase: ${testCaseContext.getTestCaseId()}")
	}
	
	@AfterTestCase
	void afterTestCase(TestCaseContext testCaseContext) {
		if (tcContexts.size() > 0) {
			TestCaseContext tcContext = tcContexts.pop()
			println("AfterTestCase: ${testCaseContext.getTestCaseId()}")
		} else {
			throw new IllegalStateException("tcContexts is empty")
		}
	}
	
	@AfterTestSuite
	void afterTestSuite(TestSuiteContext testSuiteContext) {
		if (tsContexts.size() > 0) {
			TestSuiteContext tsContext = tsContexts.pop()
			println("AfterTestSuite: ${testSuiteContext.getTestSuiteId()}")
		} else {
			throw new IllegalStateException("tsContexts is empty")
		}
	}
	
	
}