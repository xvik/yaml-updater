package ru.vyarus.yaml.config.updater.comments.model;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlTree {

    private List<YamlNode> nodes;
    private String trailingComment;

    public List<YamlNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<YamlNode> nodes) {
        this.nodes = nodes;
    }

    public String getTrailingComment() {
        return trailingComment;
    }

    public void setTrailingComment(String trailingComment) {
        this.trailingComment = trailingComment;
    }
}
