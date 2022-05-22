package io.github.benjaminsoelberg.jft;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Transformer implements ClassFileTransformer {
    private static final String FILTER_JFT_CLASSES = Utils.toNativeClassName(Transformer.class.getPackageName()) + "/";
    private final ConcurrentLinkedQueue<ClassInfo> classInfos = new ConcurrentLinkedQueue<>();
    private final Report report;
    private final List<Class<?>> classes;
    private volatile Throwable lastException;

    public Transformer(Report report, List<Class<?>> classes) {
        this.report = report;
        this.classes = classes;
    }

    @Override
    public byte[] transform(ClassLoader loader, String nativeClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // Ignore initial class load as we only want to dump classes that was previously accepted by the filter
        if (classBeingRedefined == null) {
            return null;
        }

        try {
            // Ignore our own classes
            if (nativeClassName.startsWith(FILTER_JFT_CLASSES)) {
                report.println("Ignoring %s", nativeClassName);
                return null;
            }

            // Save the class info if it previously passed the filtering
            if (classes.contains(classBeingRedefined)) {
                report.println("Dumping %s", nativeClassName);
                classInfos.add(new ClassInfo(nativeClassName, loader, protectionDomain, classfileBuffer));
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

    public Collection<ClassInfo> getClassInfos() {
        return classInfos;
    }
}
