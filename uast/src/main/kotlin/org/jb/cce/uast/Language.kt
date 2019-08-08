package org.jb.cce.uast

enum class Language(val displayName: String, private val extension: String) {
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    BASH ("Bash", "sh"),
    CSHARP("C#", "cs"),
    UNSUPPORTED("Unsupported", "");

    companion object {
        fun resolve(extension: String): Language = Language.values().find { it.extension == extension } ?: UNSUPPORTED
    }
}