package ru.vyarus.yaml.updater.parse.common.model;

import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;

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

    private final int linesCnt;

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public TreeRoot(final List<T> nodes, final int linesCnt) {
        super(null);
        getChildren().addAll(nodes);
        this.linesCnt = linesCnt;
    }

    /**
     * @return null as tree root does not have any yaml path
     */
    @Override
    public String getYamlPath() {
        return null;
    }

    /**
     * @return null as tree root does not have any yaml path
     */
    @Override
    public String getYamlPathElement() {
        return null;
    }

    /**
     * @return 0 as tree root is not represented with any line number
     */
    @Override
    public int getLineNum() {
        return 0;
    }

    /**
     * NOTE: numbers may be different for comments and structural parser as the later would ignore trailing comments.
     *
     * @return count of lines in yaml file (overall)
     */
    public int getLinesCnt() {
        return linesCnt;
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
            final String elt = child.getYamlPathElement();
            if (path.equals(elt)) {
                res = child;
            } else if (path.startsWith(elt)) {
                res = child.find(YamlModelUtils.removeLeadingPath(elt, path));
            }
            if (res != null) {
                break;
            }
        }
        return res;
    }
}
