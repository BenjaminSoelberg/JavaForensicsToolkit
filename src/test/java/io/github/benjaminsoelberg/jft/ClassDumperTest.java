package io.github.benjaminsoelberg.jft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

class ClassDumperTest {

    @Test
    void testSelfAttachCanDump() throws Exception {
        File manifestJar = createManifest(ClassDumper.class.getName());
        ClassDumper.TEST_AGENT_CMD_LINE = Utils.encodeArgs(new String[]{"-d", "target/dump.jar", "1337"});
        sun.instrument.InstrumentationImpl.loadAgent(manifestJar.getCanonicalPath());
        Assertions.assertTrue(new File("target/dump.jar").exists());
    }

    /**
     * This will create a jar in the temp dir holding the manifest to allow for self attach without the agent itself
     * having to be placed in a jar file.
     *
     * @param agentClass for where the agentmain is located
     * @return a temp jar file containing the manifest needed for testing. Note that it will be deleted upon JVM exit.
     * @throws IOException if the jar file could not be created.
     */
    @SuppressWarnings("ConcatenationWithEmptyString")
    private File createManifest(String agentClass) throws IOException {
        String manifestContent = String.format("" +
                "Manifest-Version: 1.0%n" +
                "Build-Jdk-Spec: 11%n" +
                "Launcher-Agent-Class: %1$s%n" +
                "Can-Retransform-Classes: true%n", agentClass);

        File jarFile = File.createTempFile("test-manifest", ".jar");
        jarFile.deleteOnExit();
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile))) {
            jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            jar.write(manifestContent.getBytes(StandardCharsets.UTF_8));
        }
        return jarFile;
    }
}