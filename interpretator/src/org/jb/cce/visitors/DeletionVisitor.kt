package org.jb.cce.visitors

import org.jb.cce.actions.DeleteRange
import org.jb.cce.uast.UnifiedAstRecursiveVisitor

abstract class DeletionVisitor : UnifiedAstRecursiveVisitor() {
    abstract fun getActions(): List<DeleteRange>
}