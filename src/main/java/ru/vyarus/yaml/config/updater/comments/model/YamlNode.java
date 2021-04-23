package ru.vyarus.yaml.config.updater.comments.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlNode {
    private final String padding;
    // node might be comment only!
    private String name;
    private List<String> value;
    // node comment is everything above before previous node
    // using list to avoid dealing with line separators
    private List<String> comments = new ArrayList<>();
    // property commented
    private boolean commented;
    // last node might be comment only!
    private List<YamlNode> children = new ArrayList<>();

    public YamlNode(String padding) {
        this.padding = padding;
    }

    public String getPadding() {
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

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
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

    public void setChildren(List<YamlNode> children) {
        this.children = children;
    }

    public boolean isCommentOnly() {
        return name != null;
    }
}
