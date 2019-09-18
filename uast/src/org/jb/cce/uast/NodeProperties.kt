package org.jb.cce.uast

class NodeProperties(val typeProperty: TypeProperty, val isArgument: Boolean, val isStatic: Boolean, val packagePrefix: String)

enum class TypeProperty {
    VARIABLE,
    METHOD_CALL,
    FIELD
}