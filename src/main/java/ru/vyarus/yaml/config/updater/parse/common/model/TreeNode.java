package ru.vyarus.yaml.config.updater.parse.common.model;

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

    public TreeNode(T root) {
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
     * @return map of root properties
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

    @Override
    public int getLineNum() {
        return 0;
    }

    /**
     * Search for yaml node by path. For list items path should include exact item number (e.g. list[1]), wildcard
     * search not supported.
     *
     * @param path yaml path (with '/' as separator)
     * @return found node or null
     */
    public abstract T find(String path);

    /**
     * @return all scalar properties and properties with list values (not looking inside list values!)
     */
    public abstract List<T> getTreeLeaves();
}
