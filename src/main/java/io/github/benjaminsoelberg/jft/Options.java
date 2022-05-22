package io.github.benjaminsoelberg.jft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Options {
    public static final String VERBOSE_OPTION = "-v";
    public static final String LOG_TO_STD_ERR_OPTION = "-e";
    public static final String DESTINATION_OPTION = "-d";
    public static final String FILTER_OPTION = "-f";
    public static final String INVERTED_FILTER_OPTION = "-x";
    private final ArrayList<Pattern> filter = new ArrayList<>();
    private boolean verbose;
    private boolean logToStdErr;
    private String destination;
    private boolean invertedFilter;
    private String pid;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Options(String[] args) throws ParserException {
        Iterator<String> iterator = Arrays.stream(args).iterator();
        while (iterator.hasNext()) {
            String token = iterator.next();
            if (token.startsWith("-")) {
                try {
                    switch (token) {
                        case VERBOSE_OPTION:
                            verbose = true;
                            break;
                        case LOG_TO_STD_ERR_OPTION:
                            logToStdErr = true;
                            break;
                        case DESTINATION_OPTION:
                            destination = iterator.next();
                            break;
                        case FILTER_OPTION:
                            filter.add(Pattern.compile(iterator.next()));
                            break;
                        case INVERTED_FILTER_OPTION:
                            invertedFilter = true;
                            break;
                        default:
                            throw new ParserException(String.format("Unknown option [%s]", token));
                    }
                } catch (NoSuchElementException nsee) {
                    throw new ParserException(String.format("Too few arguments for [%s]", token));
                }
            } else {
                if (iterator.hasNext()) {
                    throw new ParserException("Too many arguments");
                }
                setPid(token);
            }
        }

        if (filter.isEmpty()) {
            filter.add(Pattern.compile(".*")); // Always return true
        }

        // validate mandatory options
        if (pid == null || pid.isBlank()) {
            throw new ParserException("pid is mandatory");
        }

        // Try to create a usable (temp) destination file
        File file = null;
        try {
            if (destination == null || destination.isBlank()) {
                file = Files.createTempFile("dump-" + ProcessHandle.current().pid() + "-", ".jar").toFile();
                destination = file.getPath(); // We'll reuse it as destination filename in the agent
            } else {
                file = new File(destination);
                file.createNewFile();
            }
        } catch (IOException ioe) {
            throw new ParserException(ioe);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    /**
     * Will reproduce a well formatted list of command line args
     *
     * @return args.
     */
    public String[] getArgs() {
        ArrayList<String> args = new ArrayList<>();
        if (verbose) {
            args.add(VERBOSE_OPTION);
        }
        if (logToStdErr) {
            args.add(LOG_TO_STD_ERR_OPTION);
        }
        if (destination != null) {
            args.add(DESTINATION_OPTION);
            args.add(destination);
        }
        for (Pattern p : filter) {
            args.add(FILTER_OPTION);
            args.add(p.pattern());
        }
        if (invertedFilter) {
            args.add(INVERTED_FILTER_OPTION);
        }

        args.add(pid);

        return args.toArray(new String[0]);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isLogToStdErr() {
        return logToStdErr;
    }

    public List<Pattern> getFilter() {
        return Collections.unmodifiableList(filter);
    }

    public Predicate<String> getFilterPredicate() {
        return s -> filter.stream().anyMatch(pattern -> pattern.asMatchPredicate().test(s)) ^ invertedFilter;
    }

    public boolean isInvertedFilter() {
        return invertedFilter;
    }

    public String getDestination() {
        return destination;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
