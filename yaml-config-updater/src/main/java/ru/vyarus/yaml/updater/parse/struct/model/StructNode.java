package ru.vyarus.yaml.updater.parse.struct.model;

import ru.vyarus.yaml.updater.parse.common.model.YamlLine;

/**
 * Structure build by snakeyaml-based parser. Represents one or multiple lines in yaml file (multiple lines only in
 * case of multiline value). Usually represent some property, except list values where pure scalar might appear.
 * <p>
 * For lists, parsed structure is a bit weird for objects: dashed property goes first and later object properties
 * are children of this value (so item object become split, but this simplifies parsing (node always one or more
 * lines)).
 *
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
public class StructNode extends YamlLine<StructNode> {
    private String value;

    public StructNode(final StructNode root, final int padding, final int lineNum) {
        super(root, padding, lineNum);
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getIdentityValue() {
        // snakeyaml returns exact value
        return getValue();
    }

    @Override
    public String toString() {
        final String value = this.value == null ? "" : this.value;
        final String res;
        if (isListItem()) {
            res = "- " + (isListItemWithProperty()
                    // children may be empty in case when its consequent removes: root node removed when all child
                    // nodes removed
                    ? (hasChildren() ? getChildren().get(0) : "") : value);
        } else {
            res = getKey() + ":" + (value.isEmpty() ? "" : (" " + value));
        }
        return res;
    }
}
