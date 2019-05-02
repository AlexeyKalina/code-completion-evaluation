package org.jb.cce

enum class Language {
    JAVA, PYTHON, CSHARP, ANOTHER;

    companion object {
        fun resolve(extension: String): Language {
            return when (extension) {
                "java" -> JAVA
                "py" -> PYTHON
                "cs" -> CSHARP
                else -> ANOTHER
            }
        }
    }

    val displayName: String
        get() =
            when (this) {
                JAVA -> "Java"
                PYTHON -> "Python"
                CSHARP -> "C#"
                else -> this.name
            }
}