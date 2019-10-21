package org.jb.cce

import com.google.gson.Gson
import junit.framework.TestCase
import org.jb.cce.util.ConfigFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ConfigTests {
    companion object {
        val gson = Gson()
    }

    @DisplayName("Check default config was stored and restored correctly")
    @Test
    fun defaultConfig(@TempDir tempDir: Path) {
        val configName = "config.json"
        val directoryPath = tempDir.toAbsolutePath()
        val before = ConfigFactory.save(directoryPath.toString(), configName)
        val after = ConfigFactory.load(directoryPath.resolve(configName).toString())
        TestCase.assertEquals("Stored and restored configs are not equal", gson.toJson(before), gson.toJson(after))
    }
}