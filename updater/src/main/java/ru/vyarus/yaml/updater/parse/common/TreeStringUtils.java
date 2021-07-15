package ru.vyarus.yaml.updater.parse.common;

import java.util.Arrays;

/**
 * String utilities related to yaml model trees.
 *
 * @author Vyacheslav Rusakov
 * @since 21.05.2021
 */
public final class TreeStringUtils {

    private TreeStringUtils() {
    }

    /**
     * @param length number of whitespace characters to aggregate
     * @return string with specified number of whitespace characters
     */
    public static String whitespace(final int length) {
        String res = "";
        if (length > 0) {
            final char[] space = new char[length];
            Arrays.fill(space, ' ');
            res = String.valueOf(space);
        }
        return res;
    }

    /**
     * Appends whitespace before line.
     *
     * @param line   line to prepend whitespace
     * @param length number of whitespace chars to prepend
     * @return line with prepended whitespace
     */
    public static String shiftRight(final String line, final int length) {
        final String padding = whitespace(length);
        return padding.isEmpty() ? line : (padding + line);
    }

    /**
     * Appends whitespace to line to increase its length.
     *
     * @param line   line to increase
     * @param length target line length
     * @return line is appended whitespace (if required) not less then specified length
     */
    public static String fillTo(final String line, final int length) {
        return line.length() >= length ? line : (line + whitespace(length - line.length()));
    }
}
