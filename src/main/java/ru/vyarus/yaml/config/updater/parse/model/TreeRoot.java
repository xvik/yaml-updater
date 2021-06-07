package ru.vyarus.yaml.config.updater.parse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 30.05.2021
 */
public abstract class TreeRoot<T extends YamlLine<T>> extends TreeNode<T> {

    public TreeRoot(final T root) {
        super(root);
    }

    @Override
    public T find(final String path) {
        for (T child : getChildren()) {
            // root level property
            if (path.equals(child.getKey())) {
                return child;
            }
            // multiple levels property
            T res = child.find(path);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /**
     *
     * @return all scalar properties and properties with list values (not looking inside list values!)
     */
    @Override
    public List<T> getTreeLeaves() {
        final List<T> res = new ArrayList<>();
        for (T child: getChildren()) {
            res.addAll(child.getTreeLeaves());
        }
        return res;
    }
}
