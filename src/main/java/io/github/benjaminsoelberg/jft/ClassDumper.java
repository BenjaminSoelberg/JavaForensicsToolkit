package io.github.benjaminsoelberg.jft;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ClassDumper {

    // Public and non-final for testing purposes only.
    public static String TEST_AGENT_CMD_LINE = null;

    public static void agentmain(String cmdline, Instrumentation instrumentation) throws Exception {
        // We are unable to parse arguments to agentmain while running unit test, hence this inject-hook
        if (TEST_AGENT_CMD_LINE != null) {
            cmdline = TEST_AGENT_CMD_LINE;
        }

        String[] args = Utils.decodeArgs(cmdline);
        Options options = new Options(args);

        Report report = new Report(getHeader(), options.isVerbose(), options.isLogToStdErr());
        report.println("Agent loaded with options: %s%n", String.join(" ", args));

        report.println("Querying classes...");
        Class<?>[] classes = Arrays.stream(instrumentation.getAllLoadedClasses())
                .filter(clazz -> !clazz.isArray())
                .filter(clazz -> !clazz.isSynthetic())
                .filter(instrumentation::isModifiableClass)
                .filter(clazz -> options.getFilterPredicate().test(clazz.getName()))
                .toArray(Class[]::new);
        report.println("");

        if (classes.length == 0) {
            report.println("WARNING: No classes were found, bad filter ?%n");
        }

        // The transformer could (as a side effect by the JVM) be called with classes not in the list which is why we pass the filtered classes to it as well
        Transformer dumper = new Transformer(report, Arrays.asList(classes));

        // Invoke the transformer and remove it when filtered classes are processed
        report.println("Dumping started...");
        instrumentation.addTransformer(dumper, true);
        try {
            instrumentation.retransformClasses(classes);
        } finally {
            instrumentation.removeTransformer(dumper);
        }
        report.println("");

        // Validate that no exceptions were generated during the dump process
        if (dumper.getLastException() != null) {
            report.println("WARNING: One or more exceptions occurred while dumping classes.");
            report.dump(dumper.getLastException());
            report.println("");
        }

        File destination = new File(options.getDestination());

        report.println("Creating jar...");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(destination))) {
            dumper.getClassInfos().forEach(classInfo -> {
                try {
                    report.dump(classInfo);
                    writeZipEntry(jar, classInfo.getClassName() + ".class", classInfo.getBytecode());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writeZipEntry(jar, "report.txt", report.generate().getBytes(StandardCharsets.UTF_8));
        }
        report.println("%nDumped classes, including report.txt, can be found in: %s", options.getDestination());
    }

    private static void writeZipEntry(JarOutputStream jar, String name, byte[] data) throws IOException {
        jar.putNextEntry(new ZipEntry(name));
        jar.write(data);
    }

    private static String getHeader() {
        return "" +
                "---------------------------------------------------------%n" +
                "--> Java Forensics Toolkit v1.0.1 by Benjamin Sølberg <--%n" +
                "---------------------------------------------------------%n" +
                "https://github.com/BenjaminSoelberg/JavaForensicsToolkit%n%n";
    }

    private static void showUsage() {
        System.out.println("usage: java -jar JavaForensicsToolkit.jar [-v] [-e] [-d destination.jar] [-f filter]... [-x] <pid>");
        System.out.println();
        System.out.println("options:");
        System.out.println("-v\tverbose agent logging");
        System.out.println("-e\tagent will log to stderr instead of stdout");
        System.out.println("-d\tjar file destination of dumped classes");
        System.out.println("\tRelative paths will be relative with respect to the target process.");
        System.out.println("\tA jar file in temp will be generated if no destination was provided.");
        System.out.println("-f\tregular expression class name filter");
        System.out.println("\tCan be specified multiple times.");
        System.out.println("-x\texclude classes matching the filter");
        System.out.println("pid\tprocess id of the target java process");
        System.out.println();
        System.out.println("example:");
        System.out.println("java -jar JavaForensicsToolkit.jar -d dump.jar -f java\\\\..* -f sun\\\\..* -f jdk\\\\..* -f com\\\\.sun\\\\..* -x 123456");
    }

    /**
     * Get the absolut file location of the jar embedding this class
     *
     * @return absolut file location of jar
     */
    private static String getJarLocation() {
        URL url = ClassDumper.class.getProtectionDomain().getCodeSource().getLocation();
        String file = url.getFile();
        if (url.getProtocol().equals("file") && file.startsWith("/") && file.endsWith(".jar")) {
            return file;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        System.out.printf(getHeader());
        String absolutJarLocation = getJarLocation();
        if (args.length < 1 || absolutJarLocation == null) {
            showUsage();
            System.exit(1);
        }

        // We pre-parse the command line to be sure that it is syntactically correct prior to sending it to agentmain
        Options options = new Options(args);

        String pid = options.getPid();
        System.out.println("Injecting agent into JVM with pid: " + pid);
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            System.out.println("Dumping classes to: " + options.getDestination());
            String[] cmdLine = options.getArgs();
            vm.loadAgent(absolutJarLocation, Utils.encodeArgs(cmdLine));
        } finally {
            try {
                vm.detach();
            } catch (IOException ioe) {
                System.out.println("Unable to detach from process");
            }
        }

        System.out.println("Done");
    }

}