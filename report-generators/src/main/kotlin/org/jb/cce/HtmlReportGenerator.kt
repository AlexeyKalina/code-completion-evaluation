package org.jb.cce

import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.actions.Action
import org.jb.cce.actions.ActionSerializer
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.MetricsEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class HtmlReportGenerator(outputDir: String) {
    companion object {
        private const val globalReportName = "index.html"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"

        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        private val sessionSerializer = SessionSerializer()
        private val actionSerializer = ActionSerializer()
    }
    private lateinit var reportTitle: String

    private val reportsDir: String = Paths.get(outputDir, formatter.format(Date())).toString()
    private val resourcesDir = Paths.get(reportsDir, "res")
    private val resultsDir = Paths.get(reportsDir, "data")
    private val logsDir = Paths.get(reportsDir, "logs")
    private val actionsDir = Paths.get(reportsDir, "actions")

    private data class ResultPaths(val resourcePath: String, val reportPath: String)
    private val references: MutableMap<String, String> = mutableMapOf()

    init {
        Files.createDirectories(resourcesDir)
        Files.createDirectories(resultsDir)
        Files.createDirectories(logsDir)
        Files.createDirectories(actionsDir)
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorStyle), Paths.get(resourcesDir.toString(), tabulatorStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorScript), Paths.get(resourcesDir.toString(), tabulatorScript))
    }

    fun logsDirectory() = logsDir.toString()

    fun generateReport(sessions: List<SessionsEvaluationInfo>, metrics: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>): String {
        saveEvaluationResults(sessions)
        generateFileReports(sessions)
        generateErrorReports(errors)
        return generateGlobalReport(metrics, errors)
    }

    fun saveActions(actions: List<Action>) {
        val actionsPath = Paths.get(actionsDir.toString(), "actions.json")
        actionsPath.toFile().writeText(actionSerializer.serialize(actions))
    }

    private fun generateFileReports(evaluationResults: List<SessionsEvaluationInfo>) {
        if (evaluationResults.isEmpty()) return

        for (filePath in evaluationResults.flatMap { it.sessions.map { it.filePath } }.distinct()) {
            val sessions = evaluationResults.map { it.sessions.find { it.filePath == filePath }?.results ?: listOf() }
            val json = sessionSerializer.serialize(sessions.flatten())
            val file = File(filePath)
            val (resourcePath, reportPath) = getPaths(file.name)
            FileWriter(resourcePath).use { it.write("sessions = '$json'") }
            val report = getHtml(sessions, file.name, resourcePath,
                    evaluationResults.mapNotNull { it.sessions.find { it.filePath == filePath }?.text }.first() )
            FileWriter(reportPath).use { it.write(report) }
            references[filePath] = reportPath
        }
    }

    private fun generateErrorReports(errors: List<FileErrorInfo>) {
        for (fileError in errors) {
            val file = File(fileError.path)
            val (_, reportPath) = getPaths(file.name)
            reportTitle = "Error on actions generation for file <b>${file.name}</b>"
            val report = """<html><head><title>$reportTitle</title></head><body><h1>$reportTitle</h1>
                            <h2>Message:</h2>${fileError.exception.message}
                            <h2>StackTrace:</h2>${fileError.exception.stackTrace?.contentToString()}</body></html>"""
            FileWriter(reportPath).use { it.write(report) }
            references[file.path] = reportPath
        }
    }

    private fun generateGlobalReport(evaluationResults: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>): String {
        val sb = StringBuilder()
        reportTitle = "Code Completion Report"
        sb.appendln("<html><head><title>$reportTitle</title>")
        sb.appendln("<script src=\"res/tabulator.min.js\"></script>")
        sb.appendln("<link href=\"res/tabulator.min.css\" rel=\"stylesheet\"></head>")
        sb.appendln("<body><h1>$reportTitle</h1>")
        sb.append("<h3>${ getDistinctFiles(evaluationResults).size } file(s) successfully processed; ")
        if (errors.isEmpty()) sb.appendln("no errors occurred</h3>") else sb.appendln("${errors.size} with errors</h3>")
        sb.appendln(getMetricsTable(evaluationResults, errors))
        sb.appendln("<script>var table = new Tabulator(\"#metrics-table\", {layout:\"fitColumns\"});</script></body></html>")
        val reportPath = Paths.get(reportsDir, globalReportName).toString()
        FileWriter(reportPath).use { it.write(sb.toString()) }
        return reportPath
    }

    private fun saveEvaluationResults(evaluationResults: List<SessionsEvaluationInfo>) {
        for (results in evaluationResults) {
            val json = sessionSerializer.serialize(results)
            val dataPath = Paths.get(resultsDir.toString(), "${results.info.evaluationType}.json").toString()
            FileWriter(dataPath).use { it.write(json) }
        }
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (Files.exists(Paths.get(resourcesDir.toString(), "$fileName.js"))) {
            return getNextFilePaths(Paths.get(resourcesDir.toString(), fileName).toString())
        } else {
            ResultPaths(Paths.get(resourcesDir.toString(), "$fileName.js").toString(),
                    Paths.get(reportsDir, "$fileName.html").toString())
        }
    }

    private fun getNextFilePaths(filePath: String): ResultPaths {
        var index = 1
        do {
            index++
            val nextFile = "$filePath-$index.js"
        } while (File(nextFile).exists())
        return ResultPaths("$filePath-$index.js", "$filePath-$index.html")
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

    private fun getMetricsTable(evaluationResults: List<MetricsEvaluationInfo>, errors: List<FileErrorInfo>): String {
        val headerBuilder = StringBuilder()
        val contentBuilder = StringBuilder()

        headerBuilder.appendln("<th tabulator-formatter=\"html\">File Report</th>")
        for (metric in evaluationResults.flatMap { res -> res.globalMetrics.map { Pair(it.name, res.info.evaluationType) } }.sortedBy { it.first })
            headerBuilder.appendln("<th>${metric.first} ${metric.second}</th>")

        for (fileError in errors) {
            writeRow(contentBuilder, "<a href=\"${references[fileError.path]!!}\" style=\"color:red;\">${File(fileError.path).name}</a>",
                    evaluationResults.flatMap { it.globalMetrics.map { MetricInfo(it.name, null)} }.sortedBy { it.name })
        }

        for (filePath in getDistinctFiles(evaluationResults))
            writeRow(contentBuilder, "<a href=\"${references[filePath]!!}\">${File(filePath).name}</a>",
                evaluationResults.flatMap { it.fileMetrics.find { it.filePath == filePath }?.results ?:
                it.globalMetrics.map { MetricInfo(it.name, null)} }.sortedBy { it.name })

        writeRow(contentBuilder, "Summary", evaluationResults.flatMap { it.globalMetrics }.sortedBy { it.name })
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

    private fun writeRow(sb: StringBuilder, name: String, metrics: List<MetricInfo>) {
        sb.appendln("<tr><td>$name</td>")
        for (metric in metrics) {
            if (metric.value == null) sb.appendln("<td>----</td>")
            else sb.appendln("<td>%.3f</td>".format(metric.value))
        }
        sb.appendln("</tr>")
    }

    private fun getDistinctFiles(results: List<MetricsEvaluationInfo>): List<String> {
        return results.flatMap { it.fileMetrics.map { it.filePath } }.distinct()
    }
}