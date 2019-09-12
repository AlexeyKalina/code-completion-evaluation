package org.jb.cce.info

import org.jb.cce.Session

data class FileSessionsInfo(val filePath: String, val text: String, val sessions: List<Session>)