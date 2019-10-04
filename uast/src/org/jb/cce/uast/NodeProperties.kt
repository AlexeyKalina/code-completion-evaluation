package org.jb.cce.uast

class NodeProperties {
    companion object {
        private const val tokenTypePropertyId = "tokenType"
        private const val isArgumentPropertyId = "isArgument"
        private const val isStaticPropertyId = "isStatic"
        private const val packageNamePropertyId = "packageName"

        fun create(properties: Map<String, Any>): NodeProperties {
            val instance = NodeProperties()
            instance._properties.putAll(properties)
            return instance
        }
    }

    private val _properties = mutableMapOf<String, Any>()

    val properties: Map<String, Any> = _properties

    var tokenType: TypeProperty?
        get() = _properties[tokenTypePropertyId] as? TypeProperty
        set(value) = setProperty(tokenTypePropertyId, value)

    var isArgument: Boolean?
        get() = _properties[isArgumentPropertyId] as? Boolean
        set(value) = setProperty(isArgumentPropertyId, value)

    var isStatic: Boolean?
        get() = _properties[isStaticPropertyId] as? Boolean
        set(value) = setProperty(isStaticPropertyId, value)

    var packageName: String?
        get() = _properties[packageNamePropertyId] as? String
        set(value) = setProperty(packageNamePropertyId, value)

    private fun setProperty(id: String, value: Any?) {
        _properties[id] = value ?: throw IllegalArgumentException("Null property value. Id: $id.")
    }
}

enum class TypeProperty {
    VARIABLE,
    METHOD_CALL,
    FIELD
}