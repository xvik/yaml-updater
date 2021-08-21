package ru.vyarus.yaml.updater.parse.common.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal tree-like structure. Required to unify both yaml lines tree and object, containing root nodes.
 *
 * @param <T> child nodes type
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
public abstract class TreeNode<T extends YamlLine<T>> implements LineNumberAware {
    private T root;
    private final List<T> children = new ArrayList<>();

    public TreeNode(final T root) {
        this.root = root;
    }

    /**
     * @return root node or nul
     */
    public T getRoot() {
        return root;
    }

    /**
     * Change node root. Required for merging when nodes from new file must be included into old file tree.
     *
     * @param root new root node
     */
    public void setRoot(final T root) {
        this.root = root;
    }

    /**
     * @return children nodes
     */
    public List<T> getChildren() {
        return children;
    }

    /**
     * Note there might be comment-only nodes in case of comments parser and these are not included.
     *
     * @return map of root properties (preserving order)
     */
    public Map<String, T> getRootProperties() {
        final Map<String, T> res = new LinkedHashMap<>();
        // only real properties counted!
        for (T child : children) {
            if (child.isProperty()) {
                res.put(child.getKey(), child);
            }
        }
        return res;
    }

    /**
     * @return true if has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * @return true if contains list values as children (list property)
     */
    public boolean hasListValue() {
        return hasChildren() && getChildren().get(0).isListItem();
    }

    /**
     * Properly attached node into this node (removing node from previous root).
     *
     * @param node node to add as child
     */
    @SuppressWarnings("unchecked")
    public void add(final T node) {
        // detach from old root
        if (node.getRoot() != null) {
            node.getRoot().getChildren().remove(node);
        }
        // attach to new root
        node.setRoot((T) this);
        getChildren().add(node);
    }

    /**
     * Attaches provided nodes to current.
     *
     * @param nodes nodes to add as child
     */
    @SafeVarargs
    public final void addAll(final T... nodes) {
        for (T node : nodes) {
            add(node);
        }
    }

    /**
     * Note, for list items possible empty dash line is ignored (not a level in path).
     *
     * @return property path in yaml structure (like prop1/prop2[3]/sub) or null for tree root
     */
    public abstract String getYamlPath();

    /**
     * Cases:
     * - for property or object (or list) root:  "name"
     * - for list item: "[n]" (where n number from 0). Note that scalar value will be contained in the same node
     * and for object item it would be "empty" wrapper containing object properties as children.
     * - for comment only node: empty string (no identity)
     *
     * @return yaml path element represented by this node.
     */
    public abstract String getYamlPathElement();

    /**
     * @return all scalar properties and properties with list values (not looking inside list values!)
     */
    public abstract List<T> getTreeLeaves();

    /**
     * Search for yaml node by path. For list items path should include exact item number (e.g. list[1]), wildcard
     * search not supported.
     * <p>
     * IMPORTANT: path must NOT include node itself (node from where search started)
     *
     * @param path yaml path (with '/' as separator)
     * @return found node or null
     */
    public abstract T find(String path);
}
