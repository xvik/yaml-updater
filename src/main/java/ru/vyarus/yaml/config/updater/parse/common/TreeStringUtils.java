package ru.vyarus.yaml.config.updater.parse.common;

import java.util.Arrays;

/**
 * @author Vyacheslav Rusakov
 * @since 21.05.2021
 */
public class TreeStringUtils {

    public static String whitespace(int length) {
        String res = "";
        if (length > 0) {
            final char[] space = new char[length];
            Arrays.fill(space, ' ');
            res = String.valueOf(space);
        }
        return res;
    }

    public static String shiftRight(final String line, final int length) {
        String padding = whitespace(length);
        return padding.isEmpty() ? line : (padding + line);
    }

    public static String fillTo(final String line, final int length) {
        return line.length() >= length ? line : (line + whitespace(length - line.length()));
    }
}
