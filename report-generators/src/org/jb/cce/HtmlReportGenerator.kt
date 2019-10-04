package org.jb.cce

import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
        FileWriter(resourcePath.toString()).use { it.write("sessions = \"$json\"") }
        getHtml(
                sessions.map { it.sessionsInfo.sessions },
                fileName,
                filesDir.relativize(resourcePath).toString(),
                fileInfo.sessionsInfo.text
        ).also { html -> FileWriter(reportPath.toString()).use { it.write(html) } }
        reportReferences[fileInfo.sessionsInfo.filePath] = ReferenceInfo(reportPath, sessions.map { it.metrics }.flatten())
    }

    fun generateErrorReports(errors: List<FileErrorInfo>) {
        for (fileError in errors) {
            val filePath = Paths.get(fileError.path)
            val reportPath = getPaths(filePath.fileName.toString()).reportPath
            reportTitle = "Error on actions generation for file <b>${filePath.fileName}</b>"
            """
            |<html><head><title>$reportTitle</title></head>
            |<body><h1>$reportTitle</h1><h2>Message</h2>
            |<pre><code>${fileError.message}</code></pre>
            |<h2>StackTrace <button id='copyBtn'>&#128203</button></h2>
            |<pre><code id='stackTrace'>${fileError.stackTrace}</code></pre>
            |<script src='../res/error.js'></script></body></html>
            """.trimMargin().also { html -> FileWriter(reportPath.toString()).use { it.write(html) } }
            errorReferences[filePath.toString()] = reportPath
        }
    }

    fun generateGlobalReport(globalMetrics: List<MetricInfo>): String {
        val reportPath = Paths.get(baseDir.toString(), globalReportName).toString()
        reportTitle = "Code Completion Report"
        """
        |<html><head><title>$reportTitle</title><meta charset='utf-8'/>
        |<script src='res/tabulator.min.js'></script>
        |<link href='res/tabulator.min.css' rel='stylesheet'>
        |<link href='res/options.css' rel='stylesheet'></head>
        |<body><h1>$reportTitle</h1>
        |<h3>${reportReferences.size} file(s) successfully processed</h3>
        |<h3>${errorReferences.size} errors occurred</h3> 
        |${getToolbar(globalMetrics)}
        |${getMetricsTable(globalMetrics)}
        |<script>let table = new Tabulator('#metrics-table',{layout:'fitColumns',
        |pagination:'local',paginationSize:25,paginationSizeSelector:true,movableColumns:true,
        |dataLoaded:function(data){this.getRows()[0].freeze();this.setFilter(myFilter)}});
        |</script></body></html>
        """.trimMargin().also { html -> FileWriter(reportPath).use { it.write(html) } }
        return reportPath
    }

    private fun getPaths(fileName: String): ResultPaths {
        if (Files.exists(Paths.get(resourcesDir.toString(), "$fileName.js")))
            return getNextFilePaths(fileName)
        return ResultPaths(
                Paths.get(resourcesDir.toString(), "$fileName.js"),
                Paths.get(filesDir.toString(), "$fileName.html")
        )

    }

    private fun getNextFilePaths(fileName: String): ResultPaths {
        var index = 1
        do {
            index++
            val nextFile = Paths.get(resourcesDir.toString(), "$fileName-$index.js").toFile()
        } while (nextFile.exists())
        return ResultPaths(
                Paths.get(resourcesDir.toString(), "$fileName-$index.js"),
                Paths.get(filesDir.toString(), "$fileName-$index.html")
        )
    }

    private fun getHtml(sessions: List<List<Session>>, fileName: String, resourcePath: String, text: String): String {
        reportTitle = "Code Completion Report for file <b>$fileName</b>"
        val code = prepareCode(text, sessions)
        return """
            |<html><head><title>$reportTitle</title>
            |<script src="$resourcePath"></script>
            |<style>${style}</style></head>
            |<body><h1>$reportTitle</h1>
            |<div class="code-container">
            |<div><pre class="line-numbers">${getLineNumbers(code.lines().size)}</pre></div>
            |<div><pre class="code">$code</pre></div></div>
            |<script>${script}</script></body>
            """.trimMargin()
    }

    private fun prepareCode(text: String, _sessions: List<List<Session>>): String {
        if (_sessions.isEmpty() || _sessions.all { it.isEmpty() }) return text

        val sessions = _sessions.filterNot { it.isEmpty() }
        return StringBuilder().run {
            val delimiter = "&int;"
            val offsets = sessions.flatten().map { it.offset }.distinct().sorted()
            val sessionGroups = offsets.map { offset -> sessions.map { it.find { it.offset == offset } } }
            var offset = 0

            for (sessionGroup in sessionGroups) {
                val session = sessionGroup.filterNotNull().first()
                val commonText = escapeHtml4(text.substring(offset, session.offset))
                append(commonText)

                val center = session.expectedText.length / sessions.size
                var shift = 0
                for (j in 0 until sessionGroup.lastIndex) {
                    append(getDiv(sessionGroup[j], session.expectedText.substring(shift, shift + center)))
                    append(delimiter)
                    shift += center
                }
                append(getDiv(sessionGroup.last(), session.expectedText.substring(shift)))
                offset = session.offset + session.expectedText.length
            }
            append(escapeHtml4(text.substring(offset)))
            toString()
        }
    }

    private fun getDiv(session: Session?, text: String): String =
            "<div class='completion ${ReportColors.getColor(session, HtmlColorClasses)}' id='${session?.id}'>$text</div>"

    private fun getMetricsTable(globalMetrics: List<MetricInfo>): String {
        val sortedMetrics = globalMetrics.sortedBy { it.title }
        return """
        |<table id='metrics-table'><thead><tr>
        |<th tabulator-field='fileName' tabulator-formatter='html'>File Report</th>
        ${sortedMetrics.joinToString("\n") {
            "|<th tabulator-field='${it.title}' tabulator-sorter='number' tabulator-align='right'>${it.title}</th>"
        }}
        |</tr></thead><tbody>
        ${getRow("Summary", sortedMetrics)}
        ${errorReferences.map { errRef -> getRow(
                "<a href='${baseDir.relativize(errRef.value)}' style='color:red;'>${Paths.get(errRef.key).fileName}</a>", 
                sortedMetrics.map { MetricInfo(it.name, "—", it.evaluationType) }
        ) }.joinToString("\n")}
        ${reportReferences.map { repRef -> getRow(
                "<a href='${baseDir.relativize(repRef.value.pathToReport)}'>${File(repRef.key).name}</a>",
                sortedMetrics.map { MetricInfo(
                        it.name, 
                        repRef.value.metrics.find { that -> it.title == that.title }?.value ?: "—", 
                        it.evaluationType
                ) }
        ) }.joinToString("\n")}
        |</tbody></table>
        """.trimMargin()
    }


    private fun getRow(name: String, metrics: List<MetricInfo>): String =
            "|<tr><td>$name</td>\n|${metrics.joinToString("") { "<td>${it.value}</td>" }}</tr>"

    private fun getToolbar(globalMetrics: List<MetricInfo>): String {
        val metricNames = globalMetrics.map { it.name }.toSet().sorted()
        val evaluationTypes = globalMetrics.map { it.evaluationType }.toSet()
        val sessionsMetricIsPresent = metricNames.contains("Sessions")
        val ifSessions: (String) -> String = { if (sessionsMetricIsPresent) it else "" }
        return """
        |<div class='options'><input id='search' type='text' placeholder='Search' maxlength='50'/></div>
        |<div class='options'><button class='options-btn' id='dropdownBtn'>Metrics visibility</button>
        |<ul class='dropdown'>
        ${metricNames.joinToString("\n") { "|<li><input type='checkbox' checked onclick=\"toggleColumn('$it')\">$it</li>" }}
        |</ul></div>
        |<div class='options'><button class='options-btn' id='redrawBtn'>Redraw table</button></div>
        ${ifSessions("|<div class='options'><button class='options-btn' id='emptyRowsBtn'>Show empty rows</button></div>")}
        |<script>function toggleColumn(name){${evaluationTypes.joinToString("") { "table.toggleColumn(name+' $it');" }}}
        |let search=document.getElementById('search');search.oninput=()=>table.setFilter(myFilter);
        |let redrawBtn=document.getElementById('redrawBtn');redrawBtn.onclick=()=>table.redraw();
        ${ifSessions("""
            ||let emptyRowsBtn=document.getElementById('emptyRowsBtn');emptyRowsBtn.onclick=()=>toggleEmptyRows();
            ||let emptyHidden=()=>!emptyRowsBtn.classList.contains('active');
            ||function toggleEmptyRows(){if(emptyHidden()){
            ||emptyRowsBtn.classList.add('active');emptyRowsBtn.textContent='Hide empty rows'}
            ||else{emptyRowsBtn.classList.remove('active');emptyRowsBtn.textContent='Show empty rows'}
            ||table.setFilter(myFilter)}
            ||let toNum=(str)=>isNaN(+str)?0:+str;
            """.trimMargin())}
        |let myFilter=(data)=>(new RegExp(`.*${'$'}{search.value}.*`,'i')).test(data.fileName)
        ${ifSessions("|&&Math.max(${evaluationTypes.joinToString { "toNum(data['Sessions $it'])" }})>-!emptyHidden();")}
        |</script>
        """.trimMargin()
    }

    private fun getLineNumbers(linesCount: Int): String =
      (1..linesCount).joinToString("\n") { it.toString().padStart(linesCount.toString().length) }
  
}