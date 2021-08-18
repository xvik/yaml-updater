package ru.vyarus.yaml.updater.parse.comments.model;

import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;

import java.util.Arrays;

/**
 * Helper factory class for creating correct node objects. It is not used by main code and exists only for tests
 * and to simplify manual tree modifications in custom {@link ru.vyarus.yaml.updater.listen.UpdateListener}
 * implementations.
 * <p>
 * It is important to keep in mind that comments parser does not care of exact value, but care how many lines it takes
 * (it is important to write node back exactly as-is). Value is everything after semicolon.
 * <p>
 * Like value, comment is also stored as-is, from line start. Comment is everything above node before previous node.
 * Comment-only node (without key or value) may go last on any tree level to represent trailing comment (but, usually,
 * it's only useful for root node to represent comment at the end of file because in other cases all comment lines
 * would simply be assigned to the following node (no matter what level - in some cases impossible to track)).
 *
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
public class CmtNodeFactory {

    /**
     * Could be used to create any node type: with both key and value it would be property, without value -
     * subtree root and without both key and value - stub for virtualized list item.
     * <p>
     * PAY ATTENTION: value include EVERYTHING after colon! (including separating space)
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target line number
     * @param key     property name (may be null)
     * @param value   property value lines (may be null for subtree root)
     * @return created node
     */
    public static CmtNode createProperty(final CmtNode root,
                                         final int padding,
                                         final int lineNum,
                                         final String key,
                                         final String... value) {
        CmtNode node = new CmtNode(root, padding, lineNum);
        node.setKey(key);
        if (value != null && value.length > 0) {
            node.getValue().addAll(Arrays.asList(value));
        }
        return node;
    }

    /**
     * Creates list item node for object. Grouping node is required to preserve object structure.
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target dash line number
     * @param props list item object properties (each could be subtree)
     * @return created node
     */
    public static CmtNode createListObject(final CmtNode root,
                                           final int padding,
                                           final int lineNum,
                                           final CmtNode... props) {
        final CmtNode node = createProperty(root, padding, lineNum, null);
        YamlModelUtils.virtualListItem(node);
        node.addAll(props);
        return node;
    }

    /**
     * Creates scalar list value.
     * <p>
     * PAY ATTENTION: value include EVERYTHING after colon! (including separating space)
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target dash line number
     * @param value property value lines (may be null)
     * @return created node
     */
    public static CmtNode createListValue(final CmtNode root,
                                          final int padding,
                                          final int lineNum,
                                          final String... value) {
        final CmtNode node = createProperty(root, padding, lineNum, null, value);
        YamlModelUtils.listItem(node);
        return node;
    }
}
