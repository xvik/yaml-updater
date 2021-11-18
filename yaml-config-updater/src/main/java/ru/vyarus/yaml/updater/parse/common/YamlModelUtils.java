package ru.vyarus.yaml.updater.parse.common;

import org.yaml.snakeyaml.scanner.ScannerImpl;
import ru.vyarus.yaml.updater.parse.common.model.YamlLine;

/**
 * Yaml model utils. Aggregates logic common for both parsers.
 *
 * @author Vyacheslav Rusakov
 * @since 09.06.2021
 */
@SuppressWarnings("checkstyle:MultipleStringLiterals")
public final class YamlModelUtils {

    private YamlModelUtils() {
    }

    /**
     * Searches for correct parent node by current node padding. Used while parsing yaml file line-by-line.
     *
     * @param padding      current node padding
     * @param previousNode previously parsed node (usually prev. line)
     * @param <T>          type of target node
     * @return root node for current padding or null
     */
    public static <T extends YamlLine<T>> T findNextLineRoot(final int padding, final T previousNode) {
        T root = null;
        // not true only for getting back from subtree to root level
        if (padding > 0 && previousNode != null) {
            root = previousNode;
            while (root != null && root.getPadding() >= padding) {
                root = root.getRoot();
            }
        }
        return root;
    }

    /**
     * Marks node as list item. For scalar values, value node itself is marked as list item.
     * For object items, it must be empty line with dash (otherwise virtual object must be created to properly
     * aggregate item object).
     *
     * @param node real node to mark as list item
     * @param <T>  node type
     */
    public static <T extends YamlLine<T>> void listItem(final T node) {
        node.setListItem(true);
    }

    /**
     * Marks node as virtual list item node. Such nodes used only for grouping item object properties, when
     * first property is on the same line as dash (so without additional object it is impossible to preserve
     * hierarchy). In case of virtual node two objects repsents same yaml line: this one for dash and first child
     * is a property part.
     *
     * @param node virtual node to mark
     * @param <T>  node type
     */
    public static <T extends YamlLine<T>> void virtualListItem(final T node) {
        if (node.getKey() != null) {
            throw new IllegalArgumentException("Incorrect usage: property node can't be marked as virtual list node: "
                    + node);
        }
        node.setListItem(true);
        node.setListItemWithProperty(true);
    }

    /**
     * Removes leading part of the path (specified). Cuts off separator after specified path if required.
     *
     * @param element leading element to remove from path
     * @param path    complete path
     * @return remaining path
     */
    public static String removeLeadingPath(final String element, final String path) {
        String res = path;
        if (element == null || element.isEmpty()) {
            return res;
        }
        if (!path.startsWith(element)) {
            throw new IllegalArgumentException("Path '" + path + "' not starting with '" + element + "'");
        }
        // cut off element
        res = res.substring(element.length());
        if (!res.isEmpty() && (res.charAt(0) == YamlLine.PATH_SEPARATOR)) {
            // cut off next element separator
            res = res.substring(1);
        }
        return res;
    }

    /**
     * Property name may be quited in yaml file and contain escaped symbols. Comments parser extracts property exactly
     * as-is, but for inner comparisons cleaned version must be used. Single quoted property may contain only
     * {@code ''} single quote escape. Double-quoted property may contain escapes with backslash, including unicode
     * symbols (but not every symbol could be escaped! only supported symbols - see
     * <a href="http://yaml.org/spec/1.2-old/spec.html#id2776092">yaml spec</a>).
     * <p>
     * NOTE: cleanups should be unified with snakeyaml behaviour (same behavior)
     *
     * @param key property name to clean
     * @return cleaned property name
     */
    public static String cleanPropertyName(final String key) {
        String cleanKey = key;
        if (key != null) {
            final char first = key.charAt(0);
            final boolean singleQuote = first == '\'';
            final boolean doubleQuote = first == '"';
            if (singleQuote || doubleQuote) {
                if (key.charAt(key.length() - 1) != first) {
                    throw new IllegalStateException(
                            "Quoted property must start and end with the same quote symbol: [" + key + "]");
                }
                cleanKey = cleanKey.substring(1, cleanKey.length() - 1);
            }
            if (singleQuote) {
                // the only possible escape in single quotes
                cleanKey = cleanKey.replace("''", "'");
            }
            if (doubleQuote) {
                // only double quotes allow escaping and unicode characters
                cleanKey = unescapeDoubleQuotes(cleanKey);
            }
        }
        return cleanKey;
    }

    // see org.yaml.snakeyaml.scanner.ScannerImpl
    private static String unescapeDoubleQuotes(final String value) {
        String cleaned = value;
        int from = 1;
        int pos;

        while ((pos = cleaned.indexOf('\\', from)) > 0) {
            final char next = cleaned.charAt(pos + 1);
            if (!Character.isSupplementaryCodePoint(next)) {
                if (ScannerImpl.ESCAPE_REPLACEMENTS.containsKey(next)) {
                    // The character is one of the single-replacement
                    // types; these are replaced with a literal character
                    // from the mapping.
                    final String replacement = ScannerImpl.ESCAPE_REPLACEMENTS.get(next);
                    cleaned = cleaned.replace("\\" + next, replacement);
                    // in most cases, replaced with unicode code which would be replaced at the same position
                    continue;
                }
                if (ScannerImpl.ESCAPE_CODES.containsKey(next)) {
                    // The character is a multi-digit escape sequence, with
                    // length defined by the value in the ESCAPE_CODES map.
                    final int length = ScannerImpl.ESCAPE_CODES.get(next);
                    final String hex = value.substring(pos + 2, pos + 2 + length);
                    final int decimal = Integer.parseInt(hex, 16);
                    final String unicode = new String(Character.toChars(decimal));
                    cleaned = cleaned.replace("\\" + next + hex, unicode);
                }
            }
            from = pos + 1;
        }
        return cleaned;
    }
}
