package ru.vyarus.yaml.config.updater.parse.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for both comments and structure trees.
 *
 * @author Vyacheslav Rusakov
 * @since 30.05.2021
 */
public abstract class TreeRoot<T extends YamlLine<T>> extends TreeNode<T> {

    public TreeRoot(final List<T> nodes) {
        super(null);
        getChildren().addAll(nodes);
    }

    @Override
    public T find(final String path) {
        for (T child : getChildren()) {
            // root level property
            if (path.equals(child.getKey())) {
                return child;
            }
            // multiple levels property
            final T res = child.find(path);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public List<T> getTreeLeaves() {
        final List<T> res = new ArrayList<>();
        for (T child : getChildren()) {
            res.addAll(child.getTreeLeaves());
        }
        return res;
    }
}
