package io.github.benjaminsoelberg.jft;

import java.security.ProtectionDomain;

public class ClassInfo implements Comparable<ClassInfo> {
    private final String nativeClassName;
    private final ClassLoader classLoader;
    private final ProtectionDomain protectionDomain;
    private final byte[] bytecode;

    public ClassInfo(String nativeClassName, ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] bytecode) {
        this.nativeClassName = nativeClassName;
        this.classLoader = classLoader;
        this.protectionDomain = protectionDomain;
        this.bytecode = bytecode;
    }

    public String getNativeClassName() {
        return nativeClassName;
    }

    public String getClassName() {
        return nativeClassName.replace('/', '.');
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getClassLoaderName() {
        if (classLoader == null) {
            return Utils.toObjectId(null);
        }

        return String.format(
                "%s%s@%s",
                classLoader.getClass().getName(),
                classLoader.getName() != null ? (String.format("(%s)", classLoader.getName())) : "",
                Integer.toHexString(classLoader.hashCode()));
    }

    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    public byte[] getBytecode() {
        return bytecode;
    }

    @Override
    public String toString() {
        return String.format("%s@%s %d bytes", getClassName(), getClassLoaderName(), bytecode.length);
    }

    @Override
    public int compareTo(ClassInfo o) {
        return getNativeClassName().compareTo(o.getNativeClassName());
    }
}
