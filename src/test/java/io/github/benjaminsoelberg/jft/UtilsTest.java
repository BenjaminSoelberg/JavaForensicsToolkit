package io.github.benjaminsoelberg.jft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void testGetObjectId() {
        Assertions.assertEquals("null@0", Utils.toObjectId(null));
        Assertions.assertEquals("java.lang.String@0", Utils.toObjectId(""));
        String id = Utils.toObjectId("Hello World");
        Assertions.assertEquals("java.lang.String@", id.substring(0, id.indexOf('@') + 1));
        String hex = id.substring(id.indexOf('@') + 1);
        Assertions.assertTrue(Long.parseLong(hex, 16) > 0);
    }

    @Test
    void testExceptionToString() {
        String e = Utils.toString(new RuntimeException("Test RuntimeException"));
        String[] elements = e.split(System.lineSeparator());
        Assertions.assertEquals("java.lang.RuntimeException: Test RuntimeException", elements[0]);
        Assertions.assertEquals("\tat io.github.benjaminsoelberg.jft.UtilsTest.testExceptionToString(UtilsTest.java:", elements[1].substring(0, elements[1].length() - 3));
        Assertions.assertTrue(elements[elements.length - 1].startsWith("\tat "));
    }
}