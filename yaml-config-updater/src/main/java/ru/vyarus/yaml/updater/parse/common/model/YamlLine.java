package ru.vyarus.yaml.updater.parse.common.model;

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
 * @param <T> child nodes type
 */
public abstract class YamlLine<T extends YamlLine<T>> extends TreeNode<T> implements LineNumberAware {

    /**
     * Separator character for yaml paths. Not dot because yaml property could have dot in name.
     */
    public static final char PATH_SEPARATOR = '/';

    // line number, counting from 1
    private final int lineNum;
    private int padding;
    // property name, if property line
    private String key;

    // indicate list item: for object item all properties would be children (wrapping node),
    // for scalar items - in this object
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
            if (root.getPadding() >= padding) {
                throw new IllegalArgumentException(String.format(
                        "Child node padding (%s) can't be smaller then parent (%s) padding %s",
                        padding, root, root.getPadding()));
            }
            // line numbers could be the same in case of virtualized list object item nodes (when dash on the same
            // line as first property)
            if (root.getLineNum() > lineNum) {
                throw new IllegalArgumentException(String.format(
                        "Child node line number (%s) can't be smaller then parent (%s) line number %s",
                        lineNum, root, root.getLineNum()));
            }
            root.getChildren().add((T) this);
        }
    }

    /**
     * @return left padding of line (whitespace before)
     */
    public int getPadding() {
        return padding;
    }

    /**
     * Changes item padding. Required for merge shifts (when old and new files has different paddings and must be
     * unified).
     *
     * @param padding updated padding
     */
    public void setPadding(final int padding) {
        this.padding = padding;
    }

    /**
     * @return yaml line number in file (counting from 1)
     */
    @Override
    public int getLineNum() {
        return lineNum;
    }

    /**
     * @return property name or null if line does not represent property
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key property name
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * In all cases list item means "dash". For scalar value property and (or) value would be in the same object.
     * For object item, all properties would be children.
     *
     * @return true to indicate list item
     */
    public boolean isListItem() {
        return listItem;
    }

    /**
     * @param listItem indicate list item
     */
    public void setListItem(final boolean listItem) {
        this.listItem = listItem;
    }

    /**
     * @return true if children are object item and the first property must start on the same line as dash
     */
    public boolean isListItemWithProperty() {
        return listItemWithProperty;
    }

    /**
     * @param listItemWithProperty indicate first object property must start on the same line with dash
     */
    public void setListItemWithProperty(final boolean listItemWithProperty) {
        this.listItemWithProperty = listItemWithProperty;
    }

    /**
     * @return true for property line
     */
    public boolean isProperty() {
        return key != null;
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
        return isObjectListItem() && !isListItemWithProperty();
    }

    /**
     * Returned values assumed to be used in node comparisons.
     *
     * @return maximally accurate node value (as much as possible for used parser)
     */
    public abstract String getIdentityValue();

    @Override
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
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
     * (but not properties inside list values!)
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
