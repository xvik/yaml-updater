package ru.vyarus.yaml.config.updater.parse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for comments and structure parsers. In both cases model represent one or more yaml file lines.
 * For lists, object in list item is split to first property - list value and other props - its children
 * (this way lines hierarchy is preserved, ignoring objects consistency as not important).
 *
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public abstract class YamlLine<T extends YamlLine> {

    private final T root;
    private final int padding;
    private String key;
    private boolean listValue;
    // for list value, padding is dash padding, but this value would be a real padding
    // in all other cases it is the same as simple padding
    private int keyPadding = -1;
    private final List<T> children = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public YamlLine(final T root, final int padding) {
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

    public void setKey(final String key) {
        this.key = key;
    }

    public boolean isListValue() {
        return listValue;
    }

    public void setListValue(final boolean listValue) {
        this.listValue = listValue;
    }

    public int getKeyPadding() {
        // if not set, use main padding
        return keyPadding == -1 ? padding : keyPadding;
    }

    public void setKeyPadding(int keyPadding) {
        this.keyPadding = keyPadding;
    }

    public List<T> getChildren() {
        return children;
    }
}
