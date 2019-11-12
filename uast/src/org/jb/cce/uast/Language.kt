package org.jb.cce.uast

enum class Language(val displayName: String, private val extension: String) {
    JAVA("Java", "java"),
    PYTHON("Python", "py"),
    BASH("Shell Script", "sh"),
    ANOTHER("Another", "*"),
    UNSUPPORTED("Unsupported", ""); // TODO: There are no unsupported languages

    companion object {
        fun resolve(displayName: String): Language = values()
                .find { it.displayName.equals(displayName, ignoreCase = true) } ?: ANOTHER

        fun resolveByExtension(extension: String): Language = values().find { it.extension == extension } ?: ANOTHER
    }
}