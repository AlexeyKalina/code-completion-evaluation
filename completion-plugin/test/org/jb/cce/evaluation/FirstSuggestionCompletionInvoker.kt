package org.jb.cce.evaluation

import org.jb.cce.CompletionInvoker
import org.jb.cce.Lookup

class FirstSuggestionCompletionInvoker(private val invoker: CompletionInvoker) : CompletionInvoker by invoker {
    override fun callCompletion(expectedText: String, prefix: String): Lookup {
        val lookup = invoker.callCompletion(expectedText, prefix)
        return Lookup(lookup.text, lookup.suggestions.take(1), 0)
    }
}