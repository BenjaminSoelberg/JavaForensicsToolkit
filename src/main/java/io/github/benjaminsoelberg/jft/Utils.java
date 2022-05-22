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
}
