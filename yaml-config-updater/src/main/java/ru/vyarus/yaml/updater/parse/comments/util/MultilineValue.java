package ru.vyarus.yaml.updater.parse.comments.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yaml multi-line value detector utility.
 *
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

    /**
     * Detect multi-line marker at the end of the line.
     *
     * @param line line to check
     * @return marker descriptor or null if not found
     */
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public static Marker detect(final String line) {
        // cut off possible inline comment (prop: | # some comment)
        final String source = cutComment(line);
        Marker res = null;
        final Matcher match = MULTILINE.matcher(source);
        if (match.find()) {
            res = new Marker();
            // > or |
            res.keep = "|".equals(match.group(1));
            if (match.group(2) != null) {
                // + or -
                res.ending = "+".equals(match.group(2)) ? 1 : -1;
            }
            if (match.group(3) != null) {
                // number
                res.indent = Integer.parseInt(match.group(3));
            }
        }
        return res;
    }

    /**
     * @param value line to analyze
     * @return true if line contains any "value" that could potentially continue as flow multiline value
     */
    public static boolean couldBeFlowMultiline(final String value) {
        final String pure = cutComment(value);
        // can't accept multiline starting from empty line here, because next line could be a property or
        // list item (can't know beforehand, only after checking next line could guess multiline)
        return pure != null && !pure.isEmpty();
    }

    /**
     * Flow multiline value is a value spanning multiple lines without additional markers. The only rule is
     * the following lines should not have smaller padding.
     *
     * @param indent value indent (actually, leading property indent)
     * @return flow multiline descriptor
     */
    public static Marker flowMarker(final int indent) {
        final Marker res = new Marker();
        res.indent = indent;
        return res;
    }

    /**
     * Removes trailing yaml comment.
     *
     * @param line line to cut
     * @return line without trailing comment
     */
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

    /**
     * Multi-line value descriptor.
     */
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
