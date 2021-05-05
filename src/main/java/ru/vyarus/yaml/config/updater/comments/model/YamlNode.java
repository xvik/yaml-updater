package ru.vyarus.yaml.config.updater.comments.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlNode {
    private final YamlNode root;
    private final int padding;
    // node might be comment only!
    private String name;
    // important: value might contain comment (right comment)!
    // so even for object declaration value may exist (containing just comment)
    private List<String> value;
    // node comment is everything above before previous node
    // using list to avoid dealing with line separators
    private List<String> topComment = new ArrayList<>();
    // property commented
    private boolean commented;
    // last node might be comment only!
    private List<YamlNode> children = new ArrayList<>();
    // list value nodes are also separate objects because list node might be a sub-object
    boolean listValue;

    public YamlNode(final YamlNode root, final int padding) {
        this.root = root;
        this.padding = padding;
        if (root != null) {
            root.getChildren().add(this);
        }
    }

    public YamlNode getRoot() {
        return root;
    }

    public int getPadding() {
        return padding;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public List<String> getTopComment() {
        return topComment;
    }

    public boolean isCommented() {
        return commented;
    }

    public void setCommented(boolean commented) {
        this.commented = commented;
    }

    public List<YamlNode> getChildren() {
        return children;
    }

    public boolean isListValue() {
        return listValue;
    }

    public void setListValue(boolean listValue) {
        this.listValue = listValue;
    }

    public boolean isCommentOnly() {
        return name == null && getValue().isEmpty();
    }

    @Override
    public String toString() {
        return isCommentOnly() ? topComment.get(0) :
                (isListValue() ? " - " + value : (name + ": " + value.get(0)));
    }
}
