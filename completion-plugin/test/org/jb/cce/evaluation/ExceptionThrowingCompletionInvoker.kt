package org.jb.cce.evaluation

import org.jb.cce.CompletionInvoker
import org.jb.cce.Lookup
import java.lang.Exception

class ExceptionThrowingCompletionInvoker(private val invoker: CompletionInvoker) : CompletionInvoker by invoker {
    override fun callCompletion(expectedText: String, prefix: String): Lookup {
        throw Exception("Test Exception Message")
    }
}