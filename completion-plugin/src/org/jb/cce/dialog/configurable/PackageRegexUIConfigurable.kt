package org.jb.cce.dialog.configurable

import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.impl.PackageRegexFilter
import javax.swing.JTextField

class PackageRegexUIConfigurable(previousState: EvaluationFilter, private val layout: LayoutBuilder) : UIConfigurable {
    private val packageRegexTextField = JTextField(
            if (previousState == EvaluationFilter.ACCEPT_ALL) ""
            else (previousState as PackageRegexFilter).regex.pattern)

    override val view: Row = createView()

    override fun build(): EvaluationFilter =
            if (packageRegexTextField.text.isEmpty()) EvaluationFilter.ACCEPT_ALL
            else PackageRegexFilter(packageRegexTextField.text)

    private fun createView(): Row = layout.row {
        cell {
            label("Package Regex:")
            packageRegexTextField(growPolicy = GrowPolicy.SHORT_TEXT)
        }
    }
}