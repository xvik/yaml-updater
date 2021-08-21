package ru.vyarus.yaml.updater.parse.struct.model;

import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;

/**
 * Helper factory class for creating correct node objects. It is not used by main code and exists only for tests
 * and to simplify manual tree modifications in custom {@link ru.vyarus.yaml.updater.listen.UpdateListener}
 * implementations.
 * <p>
 * In contrast to comments parser model, structural model (snakeyaml's) use exact parsed values and ignores all
 * comments.
 *
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
public final class StructNodeFactory {

    private StructNodeFactory() {
    }

    /**
     * Could be used to create any node type: with both key and value it would be property, without value -
     * subtree root and without both key and value - stub for virtualized list item.
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target line number
     * @param key     property name (may be null)
     * @param value   property value (may be null for subtree root)
     * @return created node
     */
    public static StructNode createProperty(final StructNode root,
                                            final int padding,
                                            final int lineNum,
                                            final String key,
                                            final String value) {
        final StructNode node = new StructNode(root, padding, lineNum);
        node.setKey(key);
        node.setValue(value);
        return node;
    }

    /**
     * Creates list item node for object. Grouping node is required to preserve object structure.
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target dash line number
     * @param props   list item object properties (each could be subtree)
     * @return created node
     */
    public static StructNode createListObject(final StructNode root,
                                              final int padding,
                                              final int lineNum,
                                              final StructNode... props) {
        final StructNode node = createProperty(root, padding, lineNum, null, null);
        YamlModelUtils.virtualListItem(node);
        node.addAll(props);
        return node;
    }

    /**
     * Creates scalar list value.
     *
     * @param root    root node (may be null)
     * @param padding padding
     * @param lineNum target dash line number
     * @param value   property value lines (may be null)
     * @return created node
     */
    public static StructNode createListValue(final StructNode root,
                                             final int padding,
                                             final int lineNum,
                                             final String value) {
        final StructNode node = createProperty(root, padding, lineNum, null, value);
        YamlModelUtils.listItem(node);
        return node;
    }
}
