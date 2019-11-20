package org.jb.cce.evaluation

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.jb.cce.uast.Language
import org.jb.cce.util.FilesHelper
import org.jb.cce.util.text
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
        private const val TEST_REPORT_TYPE = "plain"
        private const val DEFAULT_REPORT_TYPE = "html"
    }
    private val projectPath = Paths.get(PROJECT_NAME).toAbsolutePath().toString()

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

    protected fun sourceFilesCount() =
            FilesHelper.getFiles(project, project.modules[0].rootManager.sourceRoots.toList()).getValue(Language.JAVA.displayName).size

    protected fun waitAfterWorkspaceCreated() = Thread.sleep(1_000)

    protected fun checkReports(workspace: EvaluationWorkspace, config: Config, reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name) {
        val check = fun(reportType: String) {
            val reports = workspace.getReports(reportType)
            TestCase.assertTrue(
                    "Report wasn't generated ($reportType)",
                    reports.isNotEmpty())
            TestCase.assertEquals(
                    "Reports don't match sessions filters ($reportType)",
                    reports.keys,
                    (config.reports.sessionsFilters + listOf(SessionsFilter.ACCEPT_ALL)).map { it.name }.toSet())
        }
        check(TEST_REPORT_TYPE)
        check(DEFAULT_REPORT_TYPE)
        val reportPath = workspace.getReports(TEST_REPORT_TYPE)[filterName]
        val reportText = FileReader(reportPath.toString()).use { it.readText() }
        val testOutput = Paths.get(projectPath, "out", reportName).toFile()
        if (testOutput.exists()) {
            val testOutputText = VfsUtil.findFileByIoFile(testOutput, false)!!.text()
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