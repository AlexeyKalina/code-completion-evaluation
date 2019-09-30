package org.jb.cce

import org.apache.commons.text.StringEscapeUtils
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.log10

class HtmlReportGenerator(outputDir: String) {
    companion object {
        private const val globalReportName = "index.html"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"
        private const val errorScript = "/error.js"
        private const val optionsStyle = "/options.css"
        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val sessionSerializer = SessionSerializer()
    }
    private lateinit var reportTitle: String

    private data class ResultPaths(val resourcePath: Path, val reportPath: Path)
    private data class ReferenceInfo(val pathToReport: Path, val metrics: List<MetricInfo>)
    private val reportReferences: MutableMap<String, ReferenceInfo> = mutableMapOf()
    private val errorReferences: MutableMap<String, Path> = mutableMapOf()

    private val baseDir = Paths.get(outputDir, "html")
    private val filesDir = Paths.get(baseDir.toString(), "files")
    private val resourcesDir = Paths.get(baseDir.toString(), "res")

    init {
        Files.createDirectories(baseDir)
        Files.createDirectories(filesDir)
        Files.createDirectories(resourcesDir)
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorStyle), Paths.get(resourcesDir.toString(), tabulatorStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorScript), Paths.get(resourcesDir.toString(), tabulatorScript))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(optionsStyle), Paths.get(resourcesDir.toString(), optionsStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(errorScript), Paths.get(resourcesDir.toString(), errorScript))
    }

    fun generateFileReport(sessions: List<FileEvaluationInfo>) {
        val json = sessionSerializer.serialize(sessions.map { it.sessionsInfo.sessions }.flatten())
        val fileInfo = sessions.first()
        val fileName = File(fileInfo.sessionsInfo.filePath).name
        val (resourcePath, reportPath) = getPaths(fileName)
        FileWriter(resourcePath.toString()).use { it.write("sessions = '$json'") }
        val report = getHtml(sessions.map { it.sessionsInfo.sessions }, fileName, filesDir.relativize(resourcePath).toString(), fileInfo.sessionsInfo.text)
        FileWriter(reportPath.toString()).use { it.write(report) }
        reportReferences[fileInfo.sessionsInfo.filePath] = ReferenceInfo(reportPath, sessions.map { it.metrics }.flatten())
    }

    fun generateErrorReports(errors: List<FileErrorInfo>) {
        for (fileError in errors) {
            val sb = StringBuilder()
            val filePath = Paths.get(fileError.path)
            reportTitle = "Error on actions generation for file <b>${filePath.fileName}</b>"
            sb.appendln("<html><head><title>$reportTitle</title></head>")
            sb.appendln("<body><h1>$reportTitle</h1><h2>Message</h2>")
            sb.appendln("<pre><code>${fileError.message}</code></pre>")
            sb.appendln("<h2>StackTrace <button id=\"copyBtn\">&#128203</button></h2>")
            sb.appendln("<pre><code id=\"stackTrace\">${fileError.stackTrace}</code></pre>")
            sb.appendln("<script src=\"../res/error.js\"></script></body></html>")
            val (_, reportPath) = getPaths(filePath.fileName.toString())
            FileWriter(reportPath.toString()).use { it.write(sb.toString()) }
            errorReferences[filePath.toString()] = reportPath
        }
    }

    fun generateGlobalReport(globalMetrics: List<MetricInfo>): String {
        val sb = StringBuilder()
        reportTitle = "Code Completion Report"
        sb.appendln("<html><head><title>$reportTitle</title>")
        sb.appendln("<script src=\"res/tabulator.min.js\"></script>")
        sb.appendln("<link href=\"res/tabulator.min.css\" rel=\"stylesheet\">")
        sb.appendln("<link href=\"res/options.css\" rel=\"stylesheet\"></head>")
        sb.appendln("<body><h1>$reportTitle</h1>")
        sb.append("<h3>${ reportReferences.size } file(s) successfully processed; ")
        if (errorReferences.isEmpty()) sb.appendln("no errors occurred</h3>") else sb.appendln("${errorReferences.size} with errors</h3>")
        sb.appendln(createToolbar(globalMetrics))
        sb.appendln(getMetricsTable(globalMetrics))
        sb.appendln("<script>let table = new Tabulator(\"#metrics-table\", {layout:\"fitColumns\",")
        sb.appendln("pagination:\"local\", paginationSize:25, paginationSizeSelector:true,")
        sb.appendln("dataLoaded:function(data){this.getRows()[0].freeze()}});</script>")
        sb.appendln("</body></html>")
        val reportPath = Paths.get(baseDir.toString(), globalReportName).toString()
        FileWriter(reportPath).use { it.write(sb.toString()) }
        return reportPath
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (Files.exists(Paths.get(resourcesDir.toString(), "$fileName.js"))) {
            return getNextFilePaths(fileName)
        } else {
            ResultPaths(Paths.get(resourcesDir.toString(), "$fileName.js"),
                    Paths.get(filesDir.toString(), "$fileName.html"))
        }
    }

    private fun getNextFilePaths(fileName: String): ResultPaths {
        var index = 1
        do {
            index++
            val nextFile = Paths.get(resourcesDir.toString(), "$fileName-$index.js").toFile()
        } while (nextFile.exists())
        return ResultPaths(Paths.get(resourcesDir.toString(), "$fileName-$index.js"), Paths.get(filesDir.toString(), "$fileName-$index.html"))
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
        val code = prepareCode(text, sessions)
        sb.appendln("<div class=\"code-container\">")
        sb.appendln("<div><pre class=\"line-numbers\">${lineNumbers(code.lines().size)}</pre></div>")
        sb.appendln("<div><pre class=\"code\">$code</pre></div>")
        sb.appendln("</div>")
        sb.appendln("<script>${script}</script>")
        sb.appendln("</body>")
    }

    private fun getHead(resourcePath: String) =
            """<head><title>$reportTitle</title>
                <script type="text/javascript" src="$resourcePath"></script>
                <style>${style}</style></head>"""

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
            val commonText = StringEscapeUtils.escapeHtml4(text.substring(offset, session.offset))
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
        sb.append(StringEscapeUtils.escapeHtml4(text.substring(offset)))
        return sb.toString()
    }

    private fun getDiv(session: Session?, text: String) : String {
        val opened = "<div class=\"completion\" id=\"${session?.id}\" style=\"color: ${ReportColors.getColor(session, HtmlColors)}; font-weight: bold\">"
        val closed = "</div>"
        return "$opened$text$closed"
    }

    private fun getMetricsTable(globalMetrics: List<MetricInfo>): String {
        val headerBuilder = StringBuilder()
        val contentBuilder = StringBuilder()
        val sortedMetrics = globalMetrics.sortedWith(compareBy({ it.name }, { it.evaluationType }))

        headerBuilder.appendln("<th tabulator-field=\"fileName\" tabulator-formatter=\"html\">File Report</th>")
        for (metric in sortedMetrics.map { "${it.name} ${it.evaluationType}" })
            headerBuilder.appendln("<th tabulator-field=\"$metric\">$metric</th>")

        writeRow(contentBuilder, "Summary", sortedMetrics)

        for (fileError in errorReferences) {
            val path = baseDir.relativize(fileError.value)
            writeRow(contentBuilder, "<a href=\"$path\" style=\"color:red;\">${Paths.get(fileError.key).fileName}</a>",
                    sortedMetrics.map { MetricInfo(it.name, null, it.evaluationType) })
        }

        for (file in reportReferences) {
            val path = baseDir.relativize(file.value.pathToReport)
            writeRow(contentBuilder,"<a href=\"$path\">${File(file.key).name}</a>",
                    sortedMetrics.map { MetricInfo(it.name, file.value.metrics.find { m ->
                        it.name == m.name && it.evaluationType == m.evaluationType }?.value, it.evaluationType) })
        }

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

    private fun createToolbar(globalMetrics: List<MetricInfo>): String {
        val sb = StringBuilder()
        sb.appendln("""<div class="options">
            <input id="search" type="text" placeholder="Search" maxlength="100"/></div>""")
        sb.appendln("""
            <div class="options">
                <button class="options-btn" id="dropdownBtn">Metrics visibility</button>
                <ul class="dropdown">
                """)
        for (metric in globalMetrics.map { it.name }.toSet().sorted())
            sb.appendln("<li><input type=\"checkbox\" checked onclick=\"toggleColumn('$metric');\">$metric</li>")
        sb.appendln("""
                </ul>
            </div>
            <div class="options">
                <button class="options-btn" id="redrawBtn" onclick="table.redraw()">Redraw table</button>
            </div>
            """)
        sb.appendln("<script> function toggleColumn(name) {")
        for (type in globalMetrics.map { it.evaluationType }.toSet())
            sb.appendln("table.toggleColumn(name + ' $type');")
        sb.appendln("} let search = document.getElementById(\"search\");")
        sb.appendln("search.oninput = function () {table.setFilter(\"fileName\", \"like\", search.value)};")
        sb.appendln("</script>")
        return sb.toString()
    }

    private fun lineNumbers(linesCount: Int): String {
        val sb = StringBuilder()
        val fullCounterLength = linesCount.toString().length
        for (counter in 1..linesCount) sb.appendln(counter.toString().padStart(fullCounterLength))
        return sb.toString()
    }
}