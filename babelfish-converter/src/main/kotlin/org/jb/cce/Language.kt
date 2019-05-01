package org.jb.cce

enum class Language {
    JAVA, PYTHON, CSHARP, ANOTHER;

    companion object {
        fun resolve(extension: String?): Language {
            return when {
                extension?.toLowerCase() == "java" -> JAVA
                extension?.toLowerCase() == "py" -> PYTHON
                extension?.toLowerCase() == "cs" -> CSHARP
                else -> ANOTHER
            }
        }
    }
}