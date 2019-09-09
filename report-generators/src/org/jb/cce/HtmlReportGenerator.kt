package org.jb.cce

import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.ReportColors.Companion.getColor
import org.jb.cce.actions.Action
import org.jb.cce.actions.ActionSerializer
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.MetricsEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class HtmlReportGenerator(outputDir: String) {
    companion object {
        private const val globalReportName = "index.html"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"
        private const val errorScript = "/error.js"
        private const val optionsStyle = "/options.css"

        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        private val sessionSerializer = SessionSerializer()
        private val actionSerializer = ActionSerializer()
    }
    private lateinit var reportTitle: String

    private val baseDir: String = Paths.get(outputDir, formatter.format(Date())).toString()
    private val resourcesDir = Paths.get(baseDir, "res")
    private val resultsDir = Paths.get(baseDir, "data")
    private val logsDir = Paths.get(baseDir, "logs")
    private val actionsDir = Paths.get(baseDir, "actions")
    private val reportsDir = Paths.get(baseDir, "reports")
    private var filesCounter = 0

    private data class ResultPaths(val resourcePath: Path, val reportPath: Path)
    private val references: MutableMap<String, Path> = mutableMapOf()

    init {
        Files.createDirectories(resourcesDir)
        Files.createDirectories(resultsDir)
        Files.createDirectories(logsDir)
        Files.createDirectories(actionsDir)
        Files.createDirectories(reportsDir)
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorStyle), Paths.get(resourcesDir.toString(), tabulatorStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorScript), Paths.get(resourcesDir.toString(), tabulatorScript))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(optionsStyle), Paths.get(resourcesDir.toString(), optionsStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(errorScript), Paths.get(resourcesDir.toString(), errorScript))
    }

    fun logsDirectory() = logsDir.toString()

    fun generateReport(sessions: List<SessionsEvaluationInfo>, metrics: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>): String {
        filesCounter = 0
        saveEvaluationResults(sessions)
        generateFileReports(sessions)
        generateErrorReports(errors)
        return generateGlobalReport(metrics, errors, sessions)
    }

    fun saveActions(actions: List<Action>, fileName: String) {
        val actionsPath = Paths.get(actionsDir.toString(), "$fileName($filesCounter).json")
        filesCounter++
        actionsPath.toFile().writeText(actionSerializer.serialize(actions))
    }

    private fun generateFileReports(evaluationResults: List<SessionsEvaluationInfo>) {
        if (evaluationResults.isEmpty()) return

        for (filePath in evaluationResults.flatMap { it.sessions.map { it.filePath } }.distinct()) {
            val sessions = evaluationResults.map { it.sessions.find { it.filePath == filePath }?.results ?: listOf() }
            val json = sessionSerializer.serialize(sessions.flatten())
            val file = File(filePath)
            val (resourcePath, reportPath) = getPaths(file.name)
            FileWriter(resourcePath.toString()).use { it.write("sessions = '$json'") }
            val report = getHtml(sessions, file.name, reportsDir.relativize(resourcePath).toString(),
                    evaluationResults.mapNotNull { it.sessions.find { it.filePath == filePath }?.text }.first() )
            FileWriter(reportPath.toString()).use { it.write(report) }
            references[filePath] = reportPath
        }
    }

    private fun generateErrorReports(errors: List<FileErrorInfo>) {
        for (fileError in errors) {
            val sb = StringBuilder()
            val file = File(fileError.path)
            reportTitle = "Error on actions generation for file <b>${file.name}</b>"
            sb.appendln("<html><head><title>$reportTitle</title></head>")
            sb.appendln("<body><h1>$reportTitle</h1><h2>Message</h2>")
            sb.appendln("<pre><code>${fileError.exception.message}</code></pre>")
            sb.appendln("<h2>StackTrace <button id=\"copyBtn\">&#128203</button></h2>")
            sb.appendln("<pre><code id=\"stackTrace\">${stackTraceToString(fileError.exception)}</code></pre>")
            sb.appendln("<script src=\"../res/error.js\"></script></body></html>")
            val (_, reportPath) = getPaths(file.name)
            FileWriter(reportPath.toString()).use { it.write(sb.toString()) }
            references[file.path] = reportPath
        }
    }

    private fun generateGlobalReport(evaluationResults: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>, sessions: List<SessionsEvaluationInfo>): String {
        val sb = StringBuilder()
        reportTitle = "Code Completion Report"
        sb.appendln("<html><head><title>$reportTitle</title>")
        sb.appendln("<script src=\"res/tabulator.min.js\"></script>")
        sb.appendln("<link href=\"res/tabulator.min.css\" rel=\"stylesheet\">")
        sb.appendln("<link href=\"res/options.css\" rel=\"stylesheet\"></head>")
        sb.appendln("<body><h1>$reportTitle</h1>")
        sb.append("<h3>${getDistinctFiles(evaluationResults).size} file(s) successfully processed; ")
        if (errors.isEmpty()) sb.appendln("no errors occurred</h3>") else sb.appendln("${errors.size} with errors</h3>")
        sb.appendln(createFilteringCheckboxes(evaluationResults))
        sb.appendln(getMetricsTable(evaluationResults, errors, sessions))
        sb.appendln("<script>var table = new Tabulator(\"#metrics-table\", {layout:\"fitColumns\"})</script>")
        sb.appendln("</body></html>")
        val reportPath = Paths.get(baseDir, globalReportName).toString()
        FileWriter(reportPath).use { it.write(sb.toString()) }
        return reportPath
    }

    private fun saveEvaluationResults(evaluationResults: List<SessionsEvaluationInfo>) {
        for (results in evaluationResults) {
            val typeFolder = Paths.get(resultsDir.toString(), results.info.evaluationType)
            Files.createDirectories(typeFolder)
            for (file in results.sessions) {
                val json = sessionSerializer.serialize(file)
                val dataPath = Paths.get(typeFolder.toString(), "${File(file.filePath).name}($filesCounter).json").toString()
                FileWriter(dataPath).use { it.write(json) }
            }
            val json = sessionSerializer.serializeConfig(results.info)
            FileWriter(Paths.get(resultsDir.toString(), "config.json").toString()).use { it.write(json) }
        }
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (Files.exists(Paths.get(resourcesDir.toString(), "$fileName.js"))) {
            return getNextFilePaths(Paths.get(resourcesDir.toString(), fileName).toString())
        } else {
            ResultPaths(Paths.get(resourcesDir.toString(), "$fileName.js"),
                    Paths.get(reportsDir.toString(), "$fileName.html"))
        }
    }

    private fun getNextFilePaths(filePath: String): ResultPaths {
        var index = 1
        do {
            index++
            val nextFile = "$filePath-$index.js"
        } while (File(nextFile).exists())
        return ResultPaths(Paths.get("$filePath-$index.js"), Paths.get("$filePath-$index.html"))
    }

    private fun getHtml(sessions: List<List<Session>>, fileName: String, resourcePath: String, text: String) : String {
        reportTitle = "Code Completion Report for file <b>$fileName</b>"
        val sb = StringBuilder()
        sb.appendln("<html>")
        sb.appendln(getHead(resourcePath))
        setBody(sb, text, sessions)
        sb.appendln("</html>")

        return sb.toString()
    }

    private fun setBody(sb: StringBuilder, text: String, sessions: List<List<Session>>) {
        sb.appendln("<body>")
        sb.appendln("<h1>$reportTitle</h1>")
        sb.appendln("<pre>")
        sb.appendln(prepareCode(text, sessions))
        sb.appendln("</pre>")
        sb.appendln("<script>$script</script>")
        sb.appendln("</body>")
    }

    private fun getHead(resourcePath: String) =
            """<head><title>$reportTitle</title>
                <script type="text/javascript" src="$resourcePath"></script>
                <style>$style</style></head>"""

    private fun prepareCode(text: String, _sessions: List<List<Session>>) : String {
        if (_sessions.isEmpty() || _sessions.all { it.isEmpty() }) return text

        val sessions = _sessions.filterNot { it.isEmpty() }
        val sb = StringBuilder()
        val delimiter = "&int;"
        val offsets = sessions.flatten().map { it.offset }.distinct().sorted()
        val sessionGroups = offsets.map { offset -> sessions.map { it.find { session -> session.offset == offset } } }
        var offset = 0

        for (sessionGroup in sessionGroups) {
            val session = sessionGroup.filterNotNull().first()
            val commonText = escapeHtml4(text.substring(offset, session.offset))
            sb.append(commonText)

            val center = session.expectedText.length / sessions.size
            var shift = 0
            for (j in 0 until sessionGroup.lastIndex) {
                sb.append(getDiv(sessionGroup[j], session.expectedText.substring(shift, shift + center)))
                sb.append(delimiter)
                shift += center
            }
            sb.append(getDiv(sessionGroup.last(), session.expectedText.substring(shift)))
            offset = session.offset + session.expectedText.length
        }
        sb.append(escapeHtml4(text.substring(offset)))
        return sb.toString()
    }

    private fun getDiv(session: Session?, text: String) : String {
        val opened = "<div class=\"completion\" id=\"${session?.id}\" style=\"color: ${getColor(session, HtmlColors)}; font-weight: bold\">"
        val closed = "</div>"
        return "$opened$text$closed"
    }

    private fun getMetricsTable(evaluationResults: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>, sessions: List<SessionsEvaluationInfo>): String {
        val headerBuilder = StringBuilder()
        val contentBuilder = StringBuilder()

        headerBuilder.appendln("<th tabulator-formatter=\"html\">File Report</th>")
        for (metric in evaluationResults.flatMap { res -> res.globalMetrics.map { "${it.name} ${res.info.evaluationType}" } }.sorted())
            headerBuilder.appendln("<th tabulator-field=\"$metric\">$metric</th>")
        for (type in evaluationResults.map { it.info.evaluationType }) headerBuilder.appendln("<th>Sessions $type</th>")

        for (fileError in errors) {
            val path = Paths.get(baseDir).relativize(references[fileError.path]!!)
            writeRow(contentBuilder, "<a href=\"$path\" style=\"color:red;\">${File(fileError.path).name}</a>",
                    evaluationResults.flatMap { it.globalMetrics.map { MetricInfo(it.name, null)} }.sortedBy { it.name },
                    evaluationResults.map { null })
        }

        for (filePath in getDistinctFiles(evaluationResults)) {
            val path = Paths.get(baseDir).relativize(references[filePath]!!)
            writeRow(contentBuilder, "<a href=\"$path\">${File(filePath).name}</a>",
                    evaluationResults.flatMap {
                        it.fileMetrics.find { it.filePath == filePath }?.results
                                ?: it.globalMetrics.map { MetricInfo(it.name, null) }
                    }.sortedBy { it.name },
                    sessions.map { it.sessions.find { it.filePath == filePath }?.results?.size })
        }

        writeRow(contentBuilder, "Summary", evaluationResults.flatMap { it.globalMetrics }.sortedBy { it.name },
                sessions.map { it.sessions.sumBy { it.results.size } })
        contentBuilder.appendln("</tr>")

        return """
            <table id="metrics-table">
                <thead>
                    <tr>
                        $headerBuilder
                    </tr>
                </thead>
                <tbody>
                    $contentBuilder
                </tbody>
            </table>
        """
    }

    private fun writeRow(sb: StringBuilder, name: String, metrics: List<MetricInfo>, sessionsCount: List<Int?>) {
        sb.appendln("<tr><td>$name</td>")
        for (metric in metrics) {
            if (metric.value == null) sb.appendln("<td>----</td>")
            else sb.appendln("<td>%.3f</td>".format(metric.value))
        }
        for (count in sessionsCount) {
            if (count == null) sb.appendln("<td>----</td>")
            else sb.appendln("<td>$count</td>")
        }
        sb.appendln("</tr>")
    }

    private fun getDistinctFiles(results: List<MetricsEvaluationInfo>): List<String> {
        return results.flatMap { it.fileMetrics.map { it.filePath } }.distinct()
    }

    private fun createFilteringCheckboxes(evaluationResults: List<MetricsEvaluationInfo>): String {
        val sb = StringBuilder()
        sb.appendln("""
            <div class="options">
                <button class="options-btn" id="dropdownBtn">Metrics visibility</button>
                <ul class="dropdown">
                """)
        for (metric in evaluationResults.first().globalMetrics.map { it.name }.sorted())
            sb.appendln("<li><input type=\"checkbox\" checked onclick=\"toggleColumn('$metric');\">$metric</li>")
        sb.appendln("""
                </ul>
            </div>
            <div class="options">
                <button class="options-btn" id="redrawBtn" onclick="table.redraw()">Redraw table</button>
            </div>
            """)
        sb.appendln("<script> function toggleColumn(name) {")
        for (type in evaluationResults.map { it.info.evaluationType }.toSet())
            sb.appendln("table.toggleColumn(name + ' $type');")
        sb.appendln("}</script>")
        return sb.toString()
    }

    private fun stackTraceToString(e: Exception): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}