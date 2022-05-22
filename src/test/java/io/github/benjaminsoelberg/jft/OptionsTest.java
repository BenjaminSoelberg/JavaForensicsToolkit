package io.github.benjaminsoelberg.jft;

import jdk.nio.Channels;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionsTest {

    public static final String FAKE_PID = "12345";

    @Test
    void testMinimalCmdLine() throws ParserException {
        Options options = new Options(new String[]{FAKE_PID});
        Assertions.assertFalse(options.isLogToStdErr());
        Assertions.assertNotNull(options.getDestination());
        Assertions.assertFalse(options.isInvertedFilter());
        Assertions.assertTrue(options.getFilterPredicate().test("any.class.name.matches"));
        Assertions.assertEquals("12345", options.getPid());
    }

    @Test
    void testLogToStdOutOption() throws ParserException {
        Options options = new Options(new String[]{FAKE_PID});
        Assertions.assertFalse(options.isLogToStdErr());
    }

    @Test
    void testLogToStdErrOption() throws ParserException {
        Options options = new Options(new String[]{"-e", FAKE_PID});
        Assertions.assertTrue(options.isLogToStdErr());
    }

    @Test
    void testVerboseOption() throws ParserException {
        Options options = new Options(new String[]{"-v", FAKE_PID});
        Assertions.assertTrue(options.isVerbose());
    }

    @Test
    void testUnknownOptionThrowsParserException() {
        Exception exception = assertThrows(ParserException.class, () -> new Options(new String[]{"-xxx", FAKE_PID}));
        Assertions.assertEquals("Unknown option [-xxx]", exception.getMessage());
    }

    @Test
    void testTooManyArgumentsThrowsParserException() {
        Exception exception = assertThrows(ParserException.class, () -> new Options(new String[]{FAKE_PID, "-xxx"}));
        Assertions.assertEquals("Too many arguments", exception.getMessage());
    }

    @Test()
    void testMissingPidThrowsParserException() {
        Exception exception = assertThrows(ParserException.class, () -> new Options(new String[]{""}));
        Assertions.assertEquals("pid is mandatory", exception.getMessage());
    }

    @Test
    void testTheFullMonty() throws ParserException {
        String[] args = new String[]{
                "-v",
                "-e",
                "-d", "/tmp/dump.jar",
                "-f", "java\\..*",
                "-f", "sun\\..*",
                "-f", "jdk\\..*",
                "-f", "com\\.sun\\..*",
                "-f", "has spaces in filter",
                "-x",
                "123456"
        };

        Options options = new Options(args);
        Assertions.assertTrue(options.isVerbose());
        Assertions.assertTrue(options.isLogToStdErr());
        Assertions.assertEquals("/tmp/dump.jar", options.getDestination());
        Assertions.assertEquals(List.of("java\\..*", "sun\\..*", "jdk\\..*", "com\\.sun\\..*", "has spaces in filter").toString(), options.getFilter().toString());
        Assertions.assertTrue(options.isInvertedFilter());
        Assertions.assertFalse(options.getFilterPredicate().test(String.class.getName()));
        Assertions.assertFalse(options.getFilterPredicate().test(Channels.class.getName()));
        Assertions.assertFalse(options.getFilterPredicate().test("sun."));
        Assertions.assertFalse(options.getFilterPredicate().test("sun.internal.magic"));
        Assertions.assertTrue(options.getFilterPredicate().test("not.sun."));
        Assertions.assertFalse(options.getFilterPredicate().test("com.sun.spark.ultra"));
        Assertions.assertFalse(options.getFilterPredicate().test("has spaces in filter"));
        Assertions.assertTrue(options.getFilterPredicate().test("has spaces in filter."));
        Assertions.assertTrue(options.getFilterPredicate().test(".has spaces in filter"));
        Assertions.assertEquals("123456", options.getPid());
    }
}