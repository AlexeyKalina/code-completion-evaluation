package org.jb.cce.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.util.EventDispatcher
import com.intellij.util.ReflectionUtil
import com.intellij.util.containers.DisposableWrapperList
import kotlin.math.min

class ListSizeRestriction private constructor(private val list: List<*>) {
  companion object {
    const val WAITING_INTERVAL_MS: Long = 1000

    // workaround for performance degradation during completion evaluation due to
    // application listeners are added more ofter than are removed
    fun applicationListeners(): ListSizeRestriction {
      val application = ApplicationManager.getApplication()
      return try {
        val dispatcher = ReflectionUtil.getField(ApplicationImpl::class.java, application,
            EventDispatcher::class.java, "myDispatcher")
        val listeners = ReflectionUtil.getField(EventDispatcher::class.java, dispatcher, DisposableWrapperList::class.java, "myListeners")
        ListSizeRestriction(listeners)
      } catch (e: ReflectiveOperationException) {
        System.err.println("WARNING: Could not extract Application listeners. Evaluation performance may become very slow.")
        ListSizeRestriction(emptyList<Any>())
      }

    }
  }

  fun waitForSize(size: Int, timeout: Int) {
    var total = 0L
    var currentSize = list.size
    val initialSize = currentSize
    while (currentSize >= size && total < timeout) {
      val waitFor = min(WAITING_INTERVAL_MS, timeout - total)
      System.err.println("List is too large. $currentSize > $size")
      Thread.sleep(waitFor)
      total += waitFor
      currentSize = list.size
    }

    if (currentSize < size && total != 0L) {
      System.err.println("List size decreased: $initialSize -> $currentSize")
    }
    if (currentSize >= size) {
      System.err.println("List still too large: $currentSize instead of $size")
    }
  }
}