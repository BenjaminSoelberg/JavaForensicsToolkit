package io.github.benjaminsoelberg.jft;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {

    private static final String HEX_CHARS = "0123456789ABCDEF";

    public static String toObjectId(Object o) {
        if (o == null) {
            return "null@0";
        }
        return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }

    public static String toString(Throwable th) {
        if (th == null) {
            return "null";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }

    public static String toHex(byte[] bytes) {
        final StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (final byte b : bytes) {
            hex.append(HEX_CHARS.charAt((b & 0xF0) >> 4)).append(HEX_CHARS.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static byte[] fromHex(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IndexOutOfBoundsException(hexString);
        }

        hexString = hexString.toUpperCase();

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) (HEX_CHARS.indexOf(hexString.charAt(i)) << 4 | HEX_CHARS.indexOf(hexString.charAt(i + 1)));
        }
        return bytes;
    }

    public static String toUtf8String(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] fromUtf8String(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String toJavaClassName(String nativeClassName) {
        return nativeClassName.replace('/', '.');
    }

    public static String toNativeClassName(String javaClassName) {
        return javaClassName.replace('.', '/');
    }

    public static String encodeArgs(String[] args) {
        return Arrays.stream(args).map(Utils::fromUtf8String).map(Utils::toHex).collect(Collectors.joining(" "));
    }

    public static String[] decodeArgs(String args) {
        return Arrays.stream(args.split(" ")).map(Utils::fromHex).map(Utils::toUtf8String).collect(Collectors.toList()).toArray(new String[]{});
    }

    @SuppressWarnings("ConstantConditions")
    public static String toClassLoaderName(ClassLoader classLoader) {
        if (classLoader == null) {
            return toClassLoaderName(classLoader, "bootloader", false);
        } else if (classLoader == ClassLoader.getPlatformClassLoader()) {
            return toClassLoaderName(classLoader, "platform", false);
        } else if (classLoader == ClassLoader.getSystemClassLoader()) {
            return toClassLoaderName(classLoader, "app", false);
        } else {
            return toClassLoaderName(classLoader, "", true);
        }
    }

    private static String toClassLoaderName(ClassLoader classLoader, String defaultName, boolean appendClassName) {
        String name = defaultName.trim();
        if (classLoader != null) {
            if (classLoader.getName() != null && !classLoader.getName().trim().isBlank()) {
                name = classLoader.getName().trim();
            }
            if (appendClassName) {
                if (!name.isBlank()) {
                    name += "_";
                }
                name += toObjectId(classLoader);
            }
        }
        return "[" + name + "]";
    }

    @SuppressWarnings("ConcatenationWithEmptyString")
    public static String getApplicationHeader() {
        return "" +
               "---------------------------------------------------------%n" +
               "--> Java Forensics Toolkit v1.1.0 by Benjamin Sølberg <--%n" +
               "---------------------------------------------------------%n" +
               "https://github.com/BenjaminSoelberg/JavaForensicsToolkit%n";
    }
}
