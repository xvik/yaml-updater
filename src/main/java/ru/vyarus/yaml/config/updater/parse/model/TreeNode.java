package ru.vyarus.yaml.config.updater.parse.model;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;

import java.util.*;

/**
 * Universal tree-like structure. Required to unify both yaml lines tree and object, containing root nodes.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
public abstract class TreeNode<T extends YamlLine<T>> {
    private final T root;
    private final List<T> children = new ArrayList<>();

    public TreeNode(T root) {
        this.root = root;
    }

    public T getRoot() {
        return root;
    }

    public List<T> getChildren() {
        return children;
    }

    public Map<String, T> getProps() {
        final Map<String, T> res = new LinkedHashMap<>();
        // only real properties counted!
        for (T child : children) {
            if (child.isProperty() && !child.isListValue()) {
                res.put(child.getKey(), child);
            }
        }
        return res;
    }
}
