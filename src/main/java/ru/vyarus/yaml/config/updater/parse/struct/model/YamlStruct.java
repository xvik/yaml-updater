package ru.vyarus.yaml.config.updater.parse.struct.model;

import ru.vyarus.yaml.config.updater.parse.model.YamlLine;

/**
 * One or multiple lines in yaml file (multiple lines only in case of multiline value). Usually represent some
 * property, except list values where pure scalar might appear.
 * <p>
 * For lists, parsed structure is a bit weird for objects: dashed property goes first and later object properties
 * are children of this value (so item object become split, but this simplifies parsing (node always one or more
 * lines)).
 *
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
public class YamlStruct extends YamlLine<YamlStruct> {
    private String value;

    public YamlStruct(final YamlStruct root, final int padding, final int lineNum) {
        super(root, padding, lineNum);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        final String value = this.value == null ? "" : this.value;
        if (isListItem()) {
            return "- " + value;
        } else {
            return getKey() + ": " + value;
        }
    }
}
