package io.github.benjaminsoelberg.jft;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Transformer implements ClassFileTransformer {
    private static final String FILTER_JFT_CLASSES = Utils.toNativeClassName(Transformer.class.getPackageName()) + "/";
    private final ConcurrentHashMap<Class<?>, byte[]> classInfos = new ConcurrentHashMap<>();
    private final Report report;
    private final List<Class<?>> expectedClasses;
    private volatile Throwable lastException;

    public Transformer(Report report, Class<?>[] expectedClasses) {
        this.report = report;
        this.expectedClasses = Arrays.asList(expectedClasses);
    }

    @Override
    public byte[] transform(ClassLoader loader, String nativeClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            // Ignore initial class load as we only want to dump classes that was previously accepted by the filter
            if (nativeClassName == null || classBeingRedefined == null || classfileBuffer == null) {
                return null;
            }

            // Ignore our own classes
            if (nativeClassName.startsWith(FILTER_JFT_CLASSES)) {
                report.println("Ignoring %s", classBeingRedefined.getName());
                return null;
            }

            // Save the class info if it previously passed the filtering
            if (expectedClasses.contains(classBeingRedefined)) {
                classInfos.computeIfAbsent(classBeingRedefined, clazz -> {
                    report.println("Dumping %s", Utils.toJavaClassName(nativeClassName));
                    return classfileBuffer;
                });
            }
        } catch (Throwable throwable) {
            // Keep latest exception for later retrieval
            lastException = throwable;
        }

        // Signal that no changes were made to the bytecode
        return null;
    }

    public Throwable getLastException() {
        return lastException;
    }

    public ConcurrentHashMap<Class<?>, byte[]> getClassInfos() {
        return classInfos;
    }
}
