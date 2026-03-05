package com.zor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZorTest {
  private static final String BASIC_SCRIPT_RESOURCE = "scripts/basic.zor";

  @Test
  void runsScriptAndPrintsExpectedOutput(@TempDir Path tempDir) throws Exception {
    var scriptFile = copyResourceScriptToTempFile(tempDir, BASIC_SCRIPT_RESOURCE);
    var output = runScriptAndCaptureStdout(scriptFile);
    assertEquals(expectedOutput(), output);
  }

  private Path copyResourceScriptToTempFile(Path tempDir, String resourcePath) throws Exception {
    var scriptFile = tempDir.resolve("basic.zor");
    try (InputStream scriptResource = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      assertNotNull(scriptResource, "Missing test resource: " + resourcePath);
      Files.copy(scriptResource, scriptFile);
    }
    return scriptFile;
  }

  private String runScriptAndCaptureStdout(Path scriptFile) throws Exception {
    var stdout = new ByteArrayOutputStream();
    var originalOut = System.out;
    try {
      System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
      Zor.main(new String[] { scriptFile.toString() });
    } finally {
      System.setOut(originalOut);
    }

    return stdout.toString(StandardCharsets.UTF_8);
  }

  private String expectedOutput() {
    return String.join(
        System.lineSeparator(),
        "6",
        "12",
        "noise bark",
        "ok",
        "");
  }
}
