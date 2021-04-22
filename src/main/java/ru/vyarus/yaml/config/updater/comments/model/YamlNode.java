package ru.vyarus.yaml.config.updater.comments.model;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlNode {
    private String padding;
    private String name;
    // node comment is everything above before previous node
    private String comment;
    // property commented
    private boolean commented;
    private List<YamlNode> children;

    public String getPadding() {
        return padding;
    }

    public void setPadding(String padding) {
        this.padding = padding;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
}
