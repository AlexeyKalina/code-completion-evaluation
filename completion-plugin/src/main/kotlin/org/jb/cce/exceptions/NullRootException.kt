package org.jb.cce.exceptions

import java.lang.NullPointerException

class NullRootException(filePath: String) : NullPointerException("For file $filePath root element not found.")