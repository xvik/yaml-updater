package ru.vyarus.yaml.config.updater.parse.comments.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public final class MultilineValue {

    /**
     * (https://yaml-multiline.info).
     * <p>
     * | - keep newlines in value
     * > - replace newlines with spaces in value (double newline replaced with newline)
     * + - append all newlines at the end (by default only one kept)
     * - - drop newlines at the end
     * \d - number of padding (counting by current property); used if first value line is more shifted
     */
    private static final Pattern MULTILINE = Pattern.compile("([|>])([+-])?(\\d+)?$");

    private MultilineValue() {
    }

    public static Marker detect(final String line) {
        // cut off possible inline comment (prop: | # some comment)
        final String source = cutComment(line);
        Marker res = null;
        final Matcher match = MULTILINE.matcher(source);
        if (match.find()) {
            res = new Marker();
            // > or |
            res.keep = match.group(1).equals("|");
            if (match.group(2) != null) {
                // + or -
                res.ending = match.group(2).equals("+") ? 1 : -1;
            }
            if (match.group(3) != null) {
                // number
                res.indent = Integer.parseInt(match.group(3));
            }
        }
        return res;
    }

    public static boolean couldBeFlowMultiline(final String value) {
        final String pure = cutComment(value);
        return pure != null && !pure.isEmpty();
    }

    public static Marker flowMarker(final int indent) {
        final Marker res = new Marker();
        res.indent = indent;
        return res;
    }

    private static String cutComment(final String line) {
        if (line == null) {
            return null;
        }
        String source = line;
        final int comment = source.indexOf('#');
        if (comment > 0) {
            source = source.substring(0, comment);
        }
        return source.trim();
    }

    @SuppressWarnings("checkstyle:VisibilityModifier")
    public static class Marker {
        // true - |, false - >
        public boolean keep;
        // 0 - default (taken by first line), 1 - +, -1 - -
        public int ending;
        // custom indent
        public int indent = -1;
    }
}
