package org.jb.cce.info

import org.jb.cce.Session

data class SessionsEvaluationInfo(val sessions: List<FileEvaluationInfo<Session>>, val info: EvaluationInfo)