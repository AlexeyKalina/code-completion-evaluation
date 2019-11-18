package org.jb.cce.evaluation

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileReader
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("Test evaluation process with different configurations")
open class EvaluationTests : ExecutionTestCase()  {
    companion object {
        private const val PROJECT_NAME = "test-project"
    }
    protected val SOURCE_FILES_COUNT = 2
    protected val projectPath = Paths.get(PROJECT_NAME).toAbsolutePath().toString()

    @TempDir
    lateinit var tempDir: Path

    override fun initOutputChecker(): OutputChecker = OutputChecker(tempDir.toString(), tempDir.toString())

    override fun getTestAppPath(): String = tempDir.toString()

    override fun getName(): String = PROJECT_NAME

    @BeforeEach
    override fun setUp() {
        File(projectPath).copyRecursively(tempDir.toFile())
        super.setUp()
    }

    @AfterEach
    override fun tearDown() = super.tearDown()

    protected fun checkReport(workspace: EvaluationWorkspace, config: Config, reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name) {
        TestCase.assertTrue(
                "Report wasn't generated",
                workspace.reports.isNotEmpty())
        TestCase.assertEquals(
                "Reports don't match sessions filters",
                workspace.reports.keys,
                (config.reports.sessionsFilters + listOf(SessionsFilter.ACCEPT_ALL)).map { it.name }.toSet())

        val reportPath = workspace.reports[filterName]
        val reportText = FileReader(reportPath.toString()).use { it.readText() }
        val testOutput = Paths.get(projectPath, "out", reportName).toFile()
        if (testOutput.exists()) {
            val testOutputText = FileReader(testOutput).use { it.readText() }
            TestCase.assertEquals(
                    "Expected and actual reports mismatched",
                    reportText,
                    testOutputText)
        } else {
            testOutput.writeText(reportText)
            fail("No expected output found. Do not forget to add the output into VCS")
        }
    }
}