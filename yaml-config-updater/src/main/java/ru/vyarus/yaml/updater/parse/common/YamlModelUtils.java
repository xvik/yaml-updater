package ru.vyarus.yaml.updater.parse.common;

import ru.vyarus.yaml.updater.parse.common.model.YamlLine;

/**
 * Yaml model utils. Aggregates logic common for both parsers.
 *
 * @author Vyacheslav Rusakov
 * @since 09.06.2021
 */
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
     * @param path complete path
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
}
