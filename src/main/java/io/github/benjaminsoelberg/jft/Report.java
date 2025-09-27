package io.github.benjaminsoelberg.jft;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class Report {
    private final ConcurrentLinkedQueue<String> lines = new ConcurrentLinkedQueue<>();
    private final boolean verbose;
    private final boolean logToStdErr;

    public Report(String header, boolean verbose, boolean logToStdErr) {
        this.verbose = verbose;
        this.logToStdErr = logToStdErr;
        println(header);
    }

    private void add(String line) {
        if (verbose) {
            if (logToStdErr) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }
        }
        lines.add(line);
    }

    public void println(String line) {
        println(line, "");
    }

    public void println(String format, Object... args) {
        add(String.format(format, args));
    }

    public void dump(Throwable throwable) {
        add(Utils.toString(throwable));
    }

    public void dump(Class<?> clazz, byte[] bytecode) {
        println("Class info: %s via %s, %d bytes", clazz.getName(), Utils.toClassLoaderName(clazz), bytecode.length);
    }

    public String generate() {
        return lines.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
