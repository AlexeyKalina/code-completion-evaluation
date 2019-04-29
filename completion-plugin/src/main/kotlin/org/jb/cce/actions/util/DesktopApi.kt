import com.intellij.openapi.diagnostic.Logger
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.util.ArrayList


object DesktopApi {
    private val LOG = Logger.getInstance(DesktopApi::class.java)

    private val os: EnumOS
        get() {
            val s = System.getProperty("os.name").toLowerCase()
            return when {
                s.contains("win") -> EnumOS.WINDOWS
                s.contains("mac") -> EnumOS.MACOS
                s.contains("solaris") -> EnumOS.SOLARIS
                s.contains("sunos") -> EnumOS.SOLARIS
                s.contains("linux") -> EnumOS.LINUX
                s.contains("unix") -> EnumOS.LINUX
                else -> EnumOS.UNKNOWN
            }
        }

    fun open(file: File): Boolean {
        if (openSystemSpecific(file.path)) return true
        return openDesktop(file)
    }

    private fun openSystemSpecific(what: String): Boolean {
        val os = os

        return when {
            os.isLinux -> when {
                runCommand("kde-open", "%s", what) -> true
                runCommand("gnome-open", "%s", what) -> true
                runCommand("xdg-open", "%s", what) -> true
                else -> false
            }
            os.isMac && runCommand("open", "%s", what) -> true
            os.isWindows && runCommand("explorer", "%s", what) -> true
            else -> false
        }
    }

    private fun openDesktop(file: File): Boolean {
        logOut("Trying to use Desktop.getDesktop().open() with $file")
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.")
                return false
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                logErr("OPEN is not supported.")
                return false
            }

            Desktop.getDesktop().open(file)

            return true
        } catch (t: Throwable) {
            logErr("Error using desktop open.", t)
            return false
        }
    }


    private fun runCommand(command: String, args: String, file: String): Boolean {
        logOut("Trying to exec:\n   cmd = $command\n   args = $args\n   %s = $file")

        val parts = prepareCommand(command, args, file)

        try {
            val p = Runtime.getRuntime().exec(parts) ?: return false

            return try {
                val retVal = p.exitValue()
                if (retVal == 0) {
                    logErr("Process ended immediately.")
                    false
                } else {
                    logErr("Process crashed.")
                    false
                }
            } catch (itse: IllegalThreadStateException) {
                logErr("Process is running.")
                true
            }

        } catch (e: IOException) {
            logErr("Error running command.", e)
            return false
        }
    }


    private fun prepareCommand(command: String, args: String?, file: String): Array<String> {
        val parts = ArrayList<String>()
        parts.add(command)

        if (args != null) {
            for (s in args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val part = String.format(s, file)
                parts.add(part.trim { it <= ' ' })
            }
        }

        return parts.toTypedArray()
    }

    private fun logErr(msg: String, t: Throwable? = null) {
        LOG.error(msg, t)
    }

    private fun logOut(msg: String) {
        LOG.debug(msg)
    }

    enum class EnumOS {
        LINUX, MACOS, SOLARIS, UNKNOWN, WINDOWS;

        val isLinux: Boolean
            get() = this == LINUX || this == SOLARIS


        val isMac: Boolean
            get() = this == MACOS


        val isWindows: Boolean
            get() = this == WINDOWS
    }
}