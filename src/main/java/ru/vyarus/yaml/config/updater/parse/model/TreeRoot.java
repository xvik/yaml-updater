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

    public T find(final String path) {
        for (T child : getChildren()) {
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
    public List<T> getAllProperties() {
        final List<T> res = new ArrayList<>();
        for (T child: getChildren()) {
            res.addAll(child.getAllProperties());
        }
        return res;
    }
}
