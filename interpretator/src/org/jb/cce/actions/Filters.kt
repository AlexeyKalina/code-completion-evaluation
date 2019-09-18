package org.jb.cce.actions

class Filters(val typeFilters: List<TypeFilter>, val argumentFilter: ArgumentFilter,
              val staticFilter: StaticFilter, val packagePrefixFilter: String)

enum class ArgumentFilter {
    ARGUMENT,
    NOT_ARGUMENT,
    ALL
}

enum class StaticFilter {
    STATIC,
    NOT_STATIC,
    ALL
}

enum class TypeFilter {
    VARIABLES,
    METHOD_CALLS,
    FIELDS
}