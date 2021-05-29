package ru.vyarus.yaml.config.updater.parse.model;

/**
 * Base class for comments and structure parsers. In both cases model represent one or more yaml file lines.
 * For lists, object in list item is split to first property - list value and other props - its children
 * (this way lines hierarchy is preserved, ignoring objects consistency as not important).
 *
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public abstract class YamlLine<T extends YamlLine<T>> extends TreeNode<T> implements LineNumberAware {

    // line number, counting from 1
    private int lineNum;
    private int padding;
    private String key;
    private boolean listValue;
    // for list value, padding is dash padding, but this value would be a real padding
    // in all other cases it is the same as simple padding
    private int keyPadding = -1;

    @SuppressWarnings("unchecked")
    public YamlLine(final T root, final int padding, final int lineNum) {
        super(root);
        this.padding = padding;
        this.lineNum = lineNum;
        if (root != null) {
            root.getChildren().add((T) this);
        }
    }

    public int getPadding() {
        return padding;
    }

    // setter remains for merge shifts, which could change padding
    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getLineNum() {
        return lineNum;
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

    public boolean isProperty() {
        return key != null;
    }

    /**
     * Note, for list items possible empty dash line is ignored (not a level in path).
     *
     * @return property path in yaml structure (like prop1/prop2[3]/sub)
     */
    public String getYamlPath() {
        final String rootPath = getRoot() != null ? getRoot().getYamlPath() : "";
        String path = rootPath;
        if (isListValue()) {
            path += "[" + getRoot().getChildren().indexOf(this) + "]";
        } else if (path.length() > 0 && getRoot().isListValue() && getRoot().isProperty()) {
            // in tree first property of list item object is a parent for other props
            // need to cut it off
            path = path.substring(0, path.lastIndexOf("/"));
        }
        if (isProperty()) {
            path += (path.isEmpty() ? "" : "/") + getKey();
        }
        return path;
    }
}
