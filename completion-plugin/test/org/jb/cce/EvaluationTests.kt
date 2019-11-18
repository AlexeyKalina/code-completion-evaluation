package org.jb.cce

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionType
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.filter.impl.*
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
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
}