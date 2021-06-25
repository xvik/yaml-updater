package ru.vyarus.yaml.config.updater.parse.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for comments and structure parsers. In both cases model represent one or more yaml file lines.
 * The only exception is lists: if list item contains object then it would be always a child of root list item
 * (and so sometimes two structure lines should be rendered as a single line: special marker in list item used
 * to indicate this).
 *
 * @author Vyacheslav Rusakov
 * @since 07.05.2021
 */
public abstract class YamlLine<T extends YamlLine<T>> extends TreeNode<T> implements LineNumberAware {

    public static final String PATH_SEPARATOR = "/";

    // line number, counting from 1
    private int lineNum;
    private int padding;
    private String key;

    private boolean listItem;
    // used for list items to identify if sub-object starts on the same line as dash
    // (in this case this virtual dash object used as sub-hierarchy grouping node)
    private boolean listItemWithProperty;

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

    public boolean isListItemWithProperty() {
        return listItemWithProperty;
    }

    public void setListItemWithProperty(boolean listItemWithProperty) {
        this.listItemWithProperty = listItemWithProperty;
    }

    public boolean isProperty() {
        return key != null;
    }

    public boolean hasListValue() {
        return hasChildren() && getChildren().get(0).isListItem();
    }

    /**
     * @return true for list items containing object (not scalar value)
     */
    public boolean isObjectListItem() {
        return isListItem() && hasChildren() && getChildren().get(0).isProperty();
    }

    /**
     * @return true when list item starts from new line and current line contains only dash
     */
    public boolean isEmptyDash() {
        return isListItem() && !isListItemWithProperty();
    }

    /**
     * Returned values assumed to be used in node comparisons.
     *
     * @return maximally accurate node value (as much as possible for used parser)
     */
    public abstract String getIdentityValue();

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
        }
        if (isProperty()) {
            path += (path.isEmpty() ? "" : PATH_SEPARATOR) + getKey();
        }
        return path;
    }

    @Override
    public T find(final String path) {
        // need to build full path to compare
        final String target = (getRoot() == null ? "" : (getYamlPath() + PATH_SEPARATOR)) + path;
        for (T child : getChildren()) {
            final String cpath = child.getYamlPath();
            T res = null;
            // compared paths are full paths (from root)
            if (target.equals(cpath)) {
                // found
                res = child;
            } else if (target.startsWith(cpath)) {
                // if child fits, searching deeper
                res = child.find(path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1));
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
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getTreeLeaves() {
        final List<T> res = new ArrayList<>();
        findProperties((T) this, res, false);
        return res;
    }

    /**
     * Same as {@link #getTreeLeaves()} but also includes scalar list items for list nodes.
     *
     * @return all scalar properties and properties with list values (but not props inside list values!)
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllPropertiesIncludingScalarLists() {
        final List<T> res = new ArrayList<>();
        findProperties((T) this, res, true);
        return res;
    }

    private void findProperties(final T node, final List<T> res, final boolean includeScalarListItems) {
        if (node.isListItem() && !node.hasChildren() && includeScalarListItems) {
            // special case: scalar list items required too
            res.add(node);
        } else if (node.hasListValue() || (!node.hasChildren() && node.isProperty())) {
            // stop on list value or leaf property (no sub objects)
            res.add(node);
            return;
        }
        node.getChildren().forEach(t -> findProperties(t, res, includeScalarListItems));
    }
}
