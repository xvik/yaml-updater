package ru.vyarus.yaml.updater.parse.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for both comments and structure trees.
 *
 * @param <T> child nodes type
 * @author Vyacheslav Rusakov
 * @since 30.05.2021
 */
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class TreeRoot<T extends YamlLine<T>> extends TreeNode<T> {

    public TreeRoot(final List<T> nodes) {
        super(null);
        getChildren().addAll(nodes);
    }

    @Override
    public String getYamlPath() {
        return null;
    }

    @Override
    public int getLineNum() {
        return 0;
    }

    @Override
    public List<T> getTreeLeaves() {
        final List<T> res = new ArrayList<>();
        for (T child : getChildren()) {
            res.addAll(child.getTreeLeaves());
        }
        return res;
    }

    @Override
    public T find(final String path) {
        T res = null;
        for (T child : getChildren()) {
            // root level property
            if (path.equals(child.getKey())) {
                res = child;
                break;
            }
            // multiple levels property
            res = child.find(path);
            if (res != null) {
                break;
            }
        }
        return res;
    }
}