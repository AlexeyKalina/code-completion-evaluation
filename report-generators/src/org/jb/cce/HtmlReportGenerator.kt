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
import kotlinx.html.*
import kotlinx.html.stream.createHTML

class HtmlReportGenerator(outputDir: String) {
    companion object {
        private const val globalReportName = "index.html"
        private const val fileScript = "/script.js"
        private const val fileStyle = "/style.css"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"
        private const val errorScript = "/error.js"
        private const val optionsStyle = "/options.css"
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

    private fun copyResources(resource: String) {
        Files.copy(
                HtmlReportGenerator::class.java.getResourceAsStream(resource),
                Paths.get(resourcesDir.toString(), resource)
        )
    }

    init {
        listOf(baseDir, filesDir, resourcesDir).map { Files.createDirectories(it) }
        listOf(fileScript, fileStyle, tabulatorScript, tabulatorStyle, errorScript, optionsStyle).map { copyResources(it) }
    }

    fun generateFileReport(sessions: List<FileEvaluationInfo>) {
        val json = sessionSerializer.serialize(sessions.map { it.sessionsInfo.sessions }.flatten())
        val fileInfo = sessions.first()
        val fileName = File(fileInfo.sessionsInfo.filePath).name
        val (resourcePath, reportPath) = getPaths(fileName)
        FileWriter(resourcePath.toString()).use { it.write("sessions = '$json'") }
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
            reportTitle = "Error on actions generation for file ${filePath.fileName}"
            createHTML().html {
                head {
                    title(reportTitle)
                }
                body {
                    h1 { +reportTitle }
                    h2 { +"Message" }
                    pre { code { +fileError.message } }
                    h2 {
                        +"Stacktrace "
                        button {
                            id = "copyBtn"
                            unsafe { raw("&#128203") }
                        }
                    }
                    pre {
                        code {
                            id = "stackTrace"
                            +fileError.stackTrace
                        }
                    }
                    script { src = "../res/error.js" }
                }
            }.also { html -> FileWriter(reportPath.toString()).use { it.write(html) } }
            errorReferences[filePath.toString()] = reportPath
        }
    }

    fun generateGlobalReport(globalMetrics: List<MetricInfo>): String {
        val reportPath = Paths.get(baseDir.toString(), globalReportName).toString()
        reportTitle = "Code Completion Report"
        createHTML().html {
            head {
                title(reportTitle)
                meta { charset = "utf-8" }
                script { src = "res/tabulator.min.js" }
                link {
                    href = "res/tabulator.min.css"
                    rel = "stylesheet"
                }
                link {
                    href = "res/options.css"
                    rel = "stylesheet"
                }
            }
            body {
                h1 { +reportTitle }
                h3 { +"${reportReferences.size} file(s) successfully processed" }
                h3 { +"${errorReferences.size} errors occurred" }
                unsafe { raw(getToolbar(globalMetrics)) }
                unsafe { raw(getMetricsTable(globalMetrics)) }
            }
        }.also { html -> FileWriter(reportPath).use { it.write(html) } }
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
        reportTitle = "Code Completion Report for file $fileName"
        val maxPrefixLength = sessions.flatMap { it.map { it.lookups.size - 1 } }.max() ?: 0
        return createHTML().html {
            head {
                title(reportTitle)
                script { src = resourcePath }
                link {
                    href = "../res/style.css"
                    rel = "stylesheet"
                }
            }
            body {
                h1 { +reportTitle }
                if (maxPrefixLength != 0) label("label") {
                    htmlFor = "prefix-length"
                    +"Prefix length:"
                }
                if (maxPrefixLength != 0) input(InputType.number) {
                    id = "prefix-length"
                    min = 0.toString()
                    max = maxPrefixLength.toString()
                    value = maxPrefixLength.toString()
                    onChange = "changePrefix()"
                }
                unsafe { raw(getCodeBlocks(text, sessions, maxPrefixLength)) }
                script { src = "../res/script.js" }
            }
        }
    }

    private fun getCodeBlocks(text: String, sessions: List<List<Session>>, maxPrefixLength: Int): String {
        return createHTML().div {
            for (prefixLength in 0..maxPrefixLength) {
                div("code-container ${if (prefixLength != maxPrefixLength) "prefix-hidden" else ""}") {
                    div { pre("line-numbers") { +getLineNumbers(text.lines().size) } }
                    div { pre("code") { unsafe { raw(prepareCode(text, sessions, prefixLength)) } } }
                }
            }
        }
    }

    private fun prepareCode(text: String, _sessions: List<List<Session>>, prefixLength: Int): String {
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
                    append(getSpan(sessionGroup[j], session.expectedText.substring(shift, shift + center), prefixLength))
                    append(delimiter)
                    shift += center
                }
                append(getSpan(sessionGroup.last(), session.expectedText.substring(shift), prefixLength))
                offset = session.offset + session.expectedText.length
            }
            append(escapeHtml4(text.substring(offset)))
            toString()
        }
    }

    private fun getSpan(session: Session?, text: String, prefixLength: Int): String =
            createHTML().span("completion ${ReportColors.getColor(session, HtmlColorClasses, prefixLength)}") {
                id = session?.id.toString()
                +text
            }

    private fun getMetricsTable(globalMetrics: List<MetricInfo>): String {
        val sortedMetrics = globalMetrics.sortedBy { it.title }
        val table = createHTML().table {
            id = "metrics-table"
            thead {
                tr {
                    th {
                        attributes["tabulator-field"] = "fileName"
                        attributes["tabulator-formatter"] = "html"
                        +"File Report"
                    }
                    sortedMetrics.map {
                        th {
                            attributes["tabulator-field"] = it.title
                            attributes["tabulator-sorter"] = "number"
                            attributes["tabulator-align"] = "right"
                            +it.title
                        }
                    }
                }
            }
            tbody {
                tr {
                    td { +"Summary" }
                    sortedMetrics.map { td { +it.value } }
                }
                for (errRef in errorReferences) {
                    tr {
                        td {
                            a(classes = "errRef") {
                                href = baseDir.relativize(errRef.value).toString()
                                +Paths.get(errRef.key).fileName.toString()
                            }
                        }
                        sortedMetrics.map { td { +"—" } }
                    }
                }
                for (repRef in reportReferences) {
                    tr {
                        td {
                            a {
                                href = baseDir.relativize(repRef.value.pathToReport).toString()
                                +File(repRef.key).name.toString()
                            }
                        }
                        sortedMetrics.map {
                            td { +(repRef.value.metrics.find { that -> it.title == that.title }?.value ?: "—") }
                        }
                    }
                }
            }
        }
        val tableScript = """
        |<script>
        |let table=new Tabulator('#metrics-table',{layout:'fitColumns',
        |pagination:'local',paginationSize:25,paginationSizeSelector:true,movableColumns:true,
        |dataLoaded:function(data){this.getRows()[0].freeze();this.setFilter(myFilter)}});
        |</script>
        |""".trimMargin()
        return table + tableScript
    }

    private fun getToolbar(globalMetrics: List<MetricInfo>): String {
        val metricNames = globalMetrics.map { it.name }.toSet().sorted()
        val evaluationTypes = globalMetrics.map { it.evaluationType }.toSet()
        val sessionMetricIsPresent = metricNames.contains("Sessions")
        val ifSessions: (String) -> String = { if (sessionMetricIsPresent) it else "" }
        val toolbar = createHTML().div {
            div("toolbar") {
                input(InputType.text) {
                    id = "search"
                    placeholder = "Search"
                    maxLength = "50"
                }
            }
            div("toolbar") {
                button(classes = "toolbarBtn") {
                    id = "dropdownBtn"
                    +"Metrics visibility"
                }
                ul("dropdown") {
                    metricNames.map {
                        li {
                            input(InputType.checkBox) {
                                checked = true
                                onClick = "toggleColumn('$it')"
                                +it
                            }
                        }
                    }
                }
            }
            div("toolbar") {
                button(classes = "toolbarBtn") {
                    id = "redrawBtn"
                    +"Redraw table"
                }
            }
            if (sessionMetricIsPresent) div("toolbar") {
                button(classes = "toolbarBtn") {
                    id = "emptyRowsBtn"
                    +"Show empty rows"
                }
            }
        }
        val toolbarScript = """
        |<script>
        |function toggleColumn(name){${evaluationTypes.joinToString("") { "table.toggleColumn(name+' $it');" }}}
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
        return toolbar + toolbarScript
    }


    private fun getLineNumbers(linesCount: Int): String =
            (1..linesCount).joinToString("\n") { it.toString().padStart(linesCount.toString().length) }

}