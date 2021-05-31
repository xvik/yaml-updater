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
public abstract class YamlLine<T extends YamlLine<T>> extends TreeNode<T> implements LineNumberAware {

    private static final String PATH_SEPARATOR = "/";

    // line number, counting from 1
    private int lineNum;
    private int padding;
    private String key;
    private boolean listItem;
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

    public boolean isListItem() {
        return listItem;
    }

    public void setListItem(final boolean listItem) {
        this.listItem = listItem;
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

    public boolean hasListValue() {
        return hasChildren() && getChildren().get(0).isListItem();
    }

    /**
     * Note, for list items possible empty dash line is ignored (not a level in path).
     *
     * @return property path in yaml structure (like prop1/prop2[3]/sub)
     */
    public String getYamlPath() {
        final String rootPath = getRoot() != null ? getRoot().getYamlPath() : "";
        String path = rootPath;
        if (isListItem()) {
            path += "[" + getRoot().getChildren().indexOf(this) + "]";
        } else if (path.length() > 0 && getRoot().isListItem() && getRoot().isProperty()) {
            // in tree first property of list item object is a parent for other props
            // need to cut it off
            path = path.substring(0, path.lastIndexOf(PATH_SEPARATOR));
        }
        if (isProperty()) {
            path += (path.isEmpty() ? "" : PATH_SEPARATOR) + getKey();
        }
        return path;
    }

    public T find(final String path) {
        // need to build full path to compare
        String target = (getRoot() == null ? "" : (getRoot().getYamlPath() + PATH_SEPARATOR)) + path;
        for (T child : getChildren()) {
            String cpath = child.getYamlPath();
            T res = null;
            // compared paths are full paths (from root)
            if (target.equals(cpath)) {
                // found
                res = child;
            } else if (target.startsWith(cpath)) {
                // if child fits, searching deeper
                res = child.find(path.substring(path.lastIndexOf(PATH_SEPARATOR)));
            }
            // if not fond checking other possibilities
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /**
     * Assumed iterative usage: first all properties selected and then list nodes are post processed (because
     * almost always list nodes processing is situation-specific).
     *
     * @return all scalar properties (but not list items) and properties with list values
     * (but not props inside list values!)
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllProperties() {
        final List<T> res = new ArrayList<>();
        findProperties((T) this, res, false);
        return res;
    }

    /**
     * Same as {@link #getAllProperties()} but also includes scalar list items fir list nodes.
     * @return all scalar properties and properties with list values (but not props inside list values!)
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllPropertiesIncludingScalarLists() {
        final List<T> res = new ArrayList<>();
        findProperties((T) this, res, true);
        return res;
    }

    private void findProperties(final T node, final List<T> res, final boolean includeScalarListItems) {
        if (node.isListItem() && !node.hasChildren() && !node.isProperty() && includeScalarListItems) {
            // special case: scalar list items required too
            res.add(node);
        } else if (node.isListItem() && node.isProperty()) {
            // special case: for list item object first item line would contain others (if no blank dash used)
            res.add(node);
        } else if (node.hasListValue() || (!node.hasChildren() && node.isProperty())) {
            // stop on list value or scalar property
            res.add(node);
            return;
        }
        node.getChildren().forEach(t -> findProperties(t, res, includeScalarListItems));
    }
}
