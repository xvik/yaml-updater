package ru.vyarus.yaml.config.updater.parse.comments.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public class MultilineValue {

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

    public static Marker detect(String line) {
        String source = line;
        // cut off possible inline comment (prop: | # some comment)
        int comment = source.indexOf('#');
        if (comment > 0) {
            source = source.substring(0, comment);
        }
        source = source.trim();
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

    public static class Marker {
        // true - |, false - >
        public boolean keep;
        // 0 - default (taken by first line), 1 - +, -1 - -
        public int ending = 0;
        // custom indent
        public int indent = -1;
    }
}
