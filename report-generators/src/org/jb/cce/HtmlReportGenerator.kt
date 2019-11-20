package org.jb.cce

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.metrics.MetricInfo
import org.jb.cce.metrics.MetricValueType
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashSet

class HtmlReportGenerator(outputDir: String, private val filterName: String): ReportGenerator {
    companion object {
        private const val globalReportName = "index.html"
        private const val fileScript = "/script.js"
        private const val fileStyle = "/style.css"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"
        private const val errorScript = "/error.js"
        private const val optionsStyle = "/options.css"
        private const val diffColumnTitle = "diff"
        private val sessionSerializer = SessionSerializer()
    }

    override val type: String = "html"

    private data class ResultPaths(val resourcePath: Path, val reportPath: Path)
    private data class ReferenceInfo(val pathToReport: Path, val metrics: List<MetricInfo>)

    private val reportReferences: MutableMap<String, ReferenceInfo> = mutableMapOf()
    private val errorReferences: MutableMap<String, Path> = mutableMapOf()

    private val filterDir = Paths.get(outputDir, type, filterName)
    private val filesDir = Paths.get(filterDir.toString(), "files")
    private val resourcesDir = Paths.get(filterDir.toString(), "res")

    private fun copyResources(resource: String) {
        Files.copy(
                HtmlReportGenerator::class.java.getResourceAsStream(resource),
                Paths.get(resourcesDir.toString(), resource))
    }

    init {
        listOf(filterDir, filesDir, resourcesDir).map { Files.createDirectories(it) }
        listOf(fileScript, fileStyle, tabulatorScript, tabulatorStyle, errorScript, optionsStyle).map { copyResources(it) }
    }

    override fun generateFileReport(sessions: List<FileEvaluationInfo>) {
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

    override fun generateErrorReports(errors: List<FileErrorInfo>) {
        for (fileError in errors) {
            val filePath = Paths.get(fileError.path)
            val reportPath = getPaths(filePath.fileName.toString()).reportPath
            val reportTitle = "Error on actions generation for file ${filePath.fileName} ($filterName filter)"
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

    override fun generateGlobalReport(globalMetrics: List<MetricInfo>): Path {
        val reportPath = Paths.get(filterDir.toString(), globalReportName)
        val reportTitle = "Code Completion Report for filter \"$filterName\""
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
                div { id = "metricsTable" }
                script { unsafe { raw(getMetricsTable(globalMetrics)) } }
            }
        }.also { html -> FileWriter(reportPath.toString()).use { it.write(html) } }
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
        val reportTitle = "Code Completion Report for file $fileName ($filterName filter)"
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

    private fun getLineNumbers(linesCount: Int): String =
            (1..linesCount).joinToString("\n") { it.toString().padStart(linesCount.toString().length) }

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
        val metricNames = globalMetrics.map { it.name }.toSet().sorted()
        val evaluationTypes = globalMetrics.map { it.evaluationType }.toSet().sorted().toMutableList()
        val manyTypes = (evaluationTypes.size > 1)
        val withDiff = (evaluationTypes.size == 2)
        if (withDiff) evaluationTypes.add(diffColumnTitle)
        var rowId = 1

        val errorMetrics = globalMetrics.map { MetricInfo(it.name, Double.NaN, it.evaluationType, it.valueType) }

        fun getReportMetrics(repRef: ReferenceInfo) = globalMetrics.map { metric ->
            MetricInfo(
                    metric.name,
                    repRef.metrics.find { it.name == metric.name && it.evaluationType == metric.evaluationType }?.value ?: Double.NaN,
                    metric.evaluationType,
                    metric.valueType
            )
        }

        fun formatMetrics(metrics: List<MetricInfo>): String = (
                if (withDiff) listOf(metrics, metrics
                        .groupBy({ it.name }, { Pair(it.value, it.valueType) })
                        .mapValues { with(it.value) { Pair(first().first - last().first, first().second) } }
                        .map { MetricInfo(it.key, it.value.first, diffColumnTitle, it.value.second) }).flatten()
                else metrics
                ).joinToString(",") { "${it.name}${it.evaluationType}:'${formatMetricValue(it.value, it.valueType)}'" }

        fun getErrorRow(errRef: Map.Entry<String, Path>): String =
                "{id:${rowId++},file:${getErrorLink(errRef)},${formatMetrics(errorMetrics)}}"

        fun getReportRow(repRef: Map.Entry<String, ReferenceInfo>) =
                "{id:${rowId++},file:${getReportLink(repRef)},${formatMetrics(getReportMetrics(repRef.value))}}"

        return """
        |let tableData = [{id:0,file:'Summary',${formatMetrics(globalMetrics)}}
        |${with(errorReferences) { if (isNotEmpty()) map { getErrorRow(it) }.joinToString(",\n", ",") else "" }}
        |${with(reportReferences) { if (isNotEmpty()) map { getReportRow(it) }.joinToString(",\n", ",") else "" }}];
        |let table=new Tabulator('#metricsTable',{data:tableData,
        |columns:[{title:'File Report',field:'file',formatter:'html'${if (manyTypes) ",width:'120'" else ""}},
        |${metricNames.joinToString(",\n") { name ->
            "{title:'$name',columns:[${evaluationTypes.joinToString(",") { type ->
                "{title:'$type',field:'${name.filter { it.isLetterOrDigit() }}$type',sorter:'number',align:'right',headerVertical:${manyTypes}}"
            }}]}"
        }}],
        |layout:'fitColumns',pagination:'local',paginationSize:25,paginationSizeSelector:true,movableColumns:true,
        |dataLoaded:function(){this.getRows()[0].freeze();this.setFilter(myFilter)}});
        """.trimMargin()
    }

    private fun formatMetricValue(value: Double, type: MetricValueType): String = when {
        value.isNaN() -> "—"
        type == MetricValueType.INT -> "${value.toInt()}"
        type == MetricValueType.DOUBLE -> "%.3f".format(Locale.US, value)
        else -> throw IllegalArgumentException("Unknown metric value type")
    }

    private fun getErrorLink(errRef: Map.Entry<String, Path>): String =
            "\"<a href='${getHtmlRelativePath(filterDir, errRef.value)}' class='errRef' target='_blank'>${Paths.get(errRef.key).fileName}</a>\""

    private fun getReportLink(repRef: Map.Entry<String, ReferenceInfo>): String =
            "\"<a href='${getHtmlRelativePath(filterDir, repRef.value.pathToReport)}' target='_blank'>${File(repRef.key).name}</a>\""

    private fun getHtmlRelativePath(base: Path, path: Path): String {
        return base.relativize(path).toString().replace(File.separatorChar, '/')
    }

    private fun getToolbar(globalMetrics: List<MetricInfo>): String {
        val metricNames = globalMetrics.map { it.name }.toSet().sorted()
        val evaluationTypes = globalMetrics.mapTo(HashSet()) { it.evaluationType }
        val withDiff = evaluationTypes.size == 2
        if (withDiff) evaluationTypes.add(diffColumnTitle)
        val sessionMetricIsPresent = metricNames.contains("Sessions")
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
                    metricNames.map { metricName ->
                        li {
                            input(InputType.checkBox) {
                                id = metricName.filter { it.isLetterOrDigit() }
                                checked = true
                                onClick = "updateCols()"
                                +metricName
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
            if (withDiff) div("toolbar") {
                button(classes = "toolbarBtn active") {
                    id = "diffBtn"
                    +"Hide diff"
                }
            }

        }
        val ifDiff: (String) -> String = { if (withDiff) it else "" }
        val ifSessions: (String) -> String = { if (sessionMetricIsPresent) it else "" }
        val filteredNames = metricNames.map { it.filter { ch -> ch.isLetterOrDigit() } }
        val toolbarScript = """|<script>
        |${filteredNames.joinToString("") { "let ${it}=document.getElementById('${it}');" }}
        |function updateCols(${ifDiff("toggleDiff=false")}){
        |${ifDiff("if(toggleDiff)diffBtn.classList.toggle('active');diffBtn.textContent=diffHidden()?'Show diff':'Hide diff';")}
        ${filteredNames.joinToString("\n") { metric ->
            """
            ||if(${metric}.checked){${evaluationTypes.joinToString("") { type -> "table.showColumn('${metric}${type}');" }}
            ||${ifDiff("if (diffHidden())table.hideColumn('${metric}${diffColumnTitle}');")}}
            ||else{${evaluationTypes.joinToString("") { type -> "table.hideColumn('${metric}${type}');" }}}
            """.trimMargin()
        }}}
        |function toggleColumn(name){${evaluationTypes.joinToString("") { "table.toggleColumn(name+'$it');" }}}
        |let search=document.getElementById('search');search.oninput=()=>table.setFilter(myFilter);
        |let redrawBtn=document.getElementById('redrawBtn');redrawBtn.onclick=()=>table.redraw();
        ${ifDiff("""
            ||let diffBtn=document.getElementById('diffBtn');
            ||diffBtn.onclick=()=>updateCols(true);
            ||let diffHidden=()=>!diffBtn.classList.contains('active');
            """.trimMargin())}
        ${ifSessions("""
            ||let emptyRowsBtn=document.getElementById('emptyRowsBtn');emptyRowsBtn.onclick=()=>toggleEmptyRows();
            ||let emptyHidden=()=>!emptyRowsBtn.classList.contains('active');
            ||function toggleEmptyRows(){if(emptyHidden()){
            ||emptyRowsBtn.classList.add('active');emptyRowsBtn.textContent='Hide empty rows'}
            ||else{emptyRowsBtn.classList.remove('active');emptyRowsBtn.textContent='Show empty rows'}
            ||table.setFilter(myFilter)}
            ||let toNum=(str)=>isNaN(+str)?0:+str;
            """.trimMargin())}
        |let myFilter=(data)=>(new RegExp(`.*${'$'}{search.value}.*`,'i')).test(data.file)
        ${ifSessions("|&&Math.max(${evaluationTypes.joinToString { "toNum(data['Sessions$it'])" }})>-!emptyHidden();")}
        |</script>""".trimMargin()
        return toolbar + toolbarScript
    }

}