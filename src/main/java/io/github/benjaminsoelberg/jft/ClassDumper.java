package io.github.benjaminsoelberg.jft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ClassDumper {

    // Public and non-final for testing purposes only.
    public static String TEST_AGENT_CMD_LINE = null;

    public static final int DUMP_BATCH_SIZE = 100;

    private final Instrumentation instrumentation;
    private final Options options;
    private final Report report;
    private final ClassTree classTree = new ClassTree();
    private final AtomicReference<Class<?>> latestDumpedClass = new AtomicReference<>();
    private final AtomicReference<Throwable> latestException = new AtomicReference<>();

    @SuppressWarnings("ReassignedVariable")
    public static void agentmain(String cmdline, Instrumentation instrumentation) throws Exception {
        /* Stage 0: Override command line options if running a unit test */
        // We are unable to parse arguments to agentmain while running unit test, hence this inject-hook
        if (TEST_AGENT_CMD_LINE != null) {
            cmdline = TEST_AGENT_CMD_LINE;
        }

        new ClassDumper(cmdline, instrumentation);
    }

    public ClassDumper(String cmdline, Instrumentation instrumentation) throws ParserException, IOException {
        this.instrumentation = instrumentation;

        /* Stage 1: decode options */
        String[] args = Utils.decodeArgs(cmdline);
        options = new Options(args);

        /* Stage 2: initialize report */
        report = new Report(Utils.getApplicationHeader(), options.isVerbose(), options.isLogToStdErr());
        report.println("Agent loaded with options: %s%n", String.join(" ", args));

        /* Stage 3: query all loaded classes */
        report.println("Querying classes...");
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(getFilteredClasses()));

        /* Stage 4: initialize transformer */
        // The transformer could (as a side effect) be called with classes not in the list which is why we pass the filtered classes list
        final ClassFileTransformer transformer = createTransformer(classes);

        if (!classes.isEmpty()) {
            /* Stage 5: add transformer */
            report.println("%d classes found.%n", classes.size());
            instrumentation.addTransformer(transformer, true);

            /* Stage 6: dump all classes in filtered list */
            report.println("Dumping classes...");
            dumpClasses(classes, transformer);

            /* Stage 7: print class loader & class tree */
            report.println("Class loader & class tree...");
            dumpNodeToReport(classTree.getRoot(), "");
        } else {
            report.println("WARNING: No classes found, bad filter ?%n");
        }

        /* Stage 8: create the jar */
        report.println("Creating jar...");
        writeJar();
    }

    private Class<?>[] getFilteredClasses() {
        return Arrays.stream(instrumentation.getAllLoadedClasses())
                .filter(instrumentation::isModifiableClass)
                .filter(clazz -> options.getFilterPredicate().test(clazz.getName()))
                .filter(clazz -> !(options.isIgnoreSystemClassloader() && clazz.getClassLoader() == null))
                .filter(clazz -> !(options.isIgnorePlatformClassloader() && clazz.getClassLoader() == ClassLoader.getPlatformClassLoader()))
                .filter(clazz -> clazz.getPackage() != this.getClass().getPackage())
                .sorted(Comparator.comparing(Class::getName))
                .toArray(Class<?>[]::new);
    }

    private ClassFileTransformer createTransformer(List<Class<?>> classes) {
        return new ClassFileTransformer() {
            @Override
            public byte[] transform(Module module, ClassLoader loader, String nativeClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                latestDumpedClass.set(null);

                try {
                    // Ignore initial class load etc. as we only want to dump classes that was previously accepted by the filter
                    if (nativeClassName == null || classBeingRedefined == null || classfileBuffer == null) {
                        return null;
                    }

                    // Save the class info if not previously processed
                    if (classes.contains(classBeingRedefined)) {
                        if (classes.remove(classBeingRedefined)) {
                            latestDumpedClass.set(classBeingRedefined);
                            report.println("Dumping %s (%d bytes)", Utils.toJavaClassName(nativeClassName), classfileBuffer.length);
                            classTree.add(classBeingRedefined, classfileBuffer);
                        }
                    }
                } catch (Throwable th) {
                    // Keep latest exception for later retrieval
                    latestException.set(th);
                }

                // Signal that no changes were made to the bytecode
                return null;
            }
        };
    }

    private void dumpClasses(List<Class<?>> classes, ClassFileTransformer transformer) {
        // Invoke the transformer and remove it when filtered classes are processed
        try {
            while (!classes.isEmpty()) {
                final Class<?>[] batch = classes.subList(0, Math.min(DUMP_BATCH_SIZE, classes.size())).toArray(new Class[0]);
                try {
                    instrumentation.retransformClasses(batch);
                } catch (ClassFormatError | InternalError ignored) {
                    // Some transformations might fail even so no bytecode was changed.
                    // And we have no other way to track which class that actually failed
                    final Class<?> ldc = latestDumpedClass.get();
                    // We only care about the classes we actually dump
                    if (ldc != null) {
                        report.println("WARNING: %s might have invalid bytecode", ldc.getName());
                    }
                } catch (Throwable th) {
                    report.println("Fatal error: Failed to dump classes");
                    report.dump(th);
                    break;
                }
            }
        } finally {
            instrumentation.removeTransformer(transformer);
        }
        report.println("");
    }

    private void writeJar() throws IOException {
        ClassTree.Node root = classTree.getRoot();
        File destination = new File(options.getDestination());
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(destination))) {
            String base = Utils.toClassLoaderName(root.getLoader()) + "/";
            dumpNodeToJar(jar, root, base);

            // Validate that no exceptions were generated during the dump process and if so display it last in the report
            Throwable th = latestException.get();
            if (th != null) {
                report.println("WARNING: One or more transformer exceptions occurred while dumping classes.");
                report.dump(th);
                report.println("");
            }

            /* Stage 9: finalize the dump */
            report.println("Done!%n%nDumped classes, including report.txt, can be found in: %s", destination.getAbsolutePath());
            writeZipEntry(jar, "report.txt", Utils.fromUtf8String(report.generate()));
        }
    }

    private void dumpNodeToJar(JarOutputStream jar, ClassTree.Node node, String base) {
        node.getClasses().forEach((clazz, bytecode) -> {
            try {
                writeZipEntry(jar, base + Utils.toNativeClassName(clazz.getName()) + ".class", bytecode);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to add %s with size %d to jar", clazz.getName(), bytecode.length), e);
            }
        });

        for (ClassTree.Node child : node.getChildren()) {
            dumpNodeToJar(jar, child, base + Utils.toClassLoaderName(child.getLoader()) + "/");
        }
    }

    private void dumpNodeToReport(ClassTree.Node node, String indentation) {
        final String indent = "    ";
        report.println(indentation + Utils.toClassLoaderName(node.getLoader()));
        node.getClasses().forEach((clazz, bytecode) -> report.println(indentation + indent + clazz.getName()));
        report.println("");
        node.getChildren().forEach(child -> dumpNodeToReport(child, indentation + indent));
    }

    private void writeZipEntry(JarOutputStream jar, String name, byte[] data) throws IOException {
        jar.putNextEntry(new ZipEntry(name));
        jar.write(data);
    }

}