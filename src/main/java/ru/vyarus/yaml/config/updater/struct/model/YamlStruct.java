package ru.vyarus.yaml.config.updater.struct.model;

import java.util.ArrayList;
import java.util.List;

/**
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
