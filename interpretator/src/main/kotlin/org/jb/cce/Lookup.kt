package org.jb.cce

class Lookup(val text: String,
             val suggests: List<Suggest>,
             val success: Boolean,
             val latency: Long)