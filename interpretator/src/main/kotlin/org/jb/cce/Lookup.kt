package org.jb.cce

class Lookup(val text: String,
             val suggestions: List<Suggestion>,
             val success: Boolean,
             val latency: Long)