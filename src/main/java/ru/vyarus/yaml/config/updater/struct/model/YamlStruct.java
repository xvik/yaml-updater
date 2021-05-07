package ru.vyarus.yaml.config.updater.struct.model;

import java.util.ArrayList;
import java.util.List;

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
public class YamlStruct {
    private final YamlStruct root;
    private final int padding;
    private String name;
    private String value;
    private boolean listValue;
    private List<YamlStruct> children = new ArrayList<>();

    public YamlStruct(final YamlStruct root, final int padding) {
        this.root = root;
        this.padding = padding;
        if (root != null) {
            root.getChildren().add(this);
        }
    }

    public YamlStruct getRoot() {
        return root;
    }

    public int getPadding() {
        return padding;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isListValue() {
        return listValue;
    }

    public void setListValue(boolean listValue) {
        this.listValue = listValue;
    }

    public List<YamlStruct> getChildren() {
        return children;
    }
}
