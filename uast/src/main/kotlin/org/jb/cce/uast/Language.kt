package org.jb.cce.uast

enum class Language(val displayName: String, private val extension: String) {
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    BASH("Shell Script", "sh"),
    CSHARP("C#", "cs"),
    ANOTHER("Another", "*"),
    UNSUPPORTED("Unsupported", "");

    companion object {
        fun resolve(displayName: String): Language = values().find { it.displayName == displayName } ?: ANOTHER
    }
}