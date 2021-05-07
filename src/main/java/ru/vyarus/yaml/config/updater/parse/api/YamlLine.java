package ru.vyarus.yaml.config.updater.parse.api;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public class YamlLine<T extends YamlLine> {

    private final T root;
    private final int padding;
    private String key;
    private boolean listValue;
    private List<T> children = new ArrayList<>();

    public YamlLine(T root, int padding) {
        this.root = root;
        this.padding = padding;
        if (root != null) {
            root.getChildren().add(this);
        }
    }

    public T getRoot() {
        return root;
    }

    public int getPadding() {
        return padding;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isListValue() {
        return listValue;
    }

    public void setListValue(boolean listValue) {
        this.listValue = listValue;
    }

    public List<T> getChildren() {
        return children;
    }
}
