package org.jb.cce.evaluation

import org.jb.cce.SessionsFilter
import org.jb.cce.info.FileSessionsInfo
import org.jb.cce.storages.SessionsStorage

class FilteredSessionsStorage(private val filter: SessionsFilter, storage: SessionsStorage) : SessionsStorage(storage.storageDir) {
    override fun getSessions(path: String): FileSessionsInfo {
        val sessionsInfo = super.getSessions(path)
        val filteredSessions = filter.apply(sessionsInfo.sessions)
        return FileSessionsInfo(sessionsInfo.filePath, sessionsInfo.text, filteredSessions)
    }
}