package org.jb.cce.filters

import com.intellij.openapi.project.Project
import org.jb.cce.actions.Filters
import javax.swing.BoxLayout
import javax.swing.JPanel

class FiltersFactory(project: Project) {
    private val typesFactory = TypeFilterFactory()
    private val staticFactory = StaticFilterFactory()
    private val argumentFactory = ArgumentFilterFactory()
    private val packagePrefixFactory = PackagePrefixFilterFactory(project)

    val panel = createPanel()

    fun getFilters() = Filters(typesFactory.getFilter(), argumentFactory.getFilter(), staticFactory.getFilter(), packagePrefixFactory.getFilter())

    private fun createPanel(): JPanel {
        val filtersPanel = JPanel()
        filtersPanel.layout = BoxLayout(filtersPanel, BoxLayout.Y_AXIS)
        filtersPanel.add(typesFactory.panel)
        filtersPanel.add(staticFactory.panel)
        filtersPanel.add(argumentFactory.panel)
        filtersPanel.add(packagePrefixFactory.panel)
        return filtersPanel
    }
}

