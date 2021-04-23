package ru.vyarus.yaml.config.updater.comments.model;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlTree {

    private final List<YamlNode> nodes;

    public YamlTree(final List<YamlNode> nodes) {
        this.nodes = nodes;
    }

    public List<YamlNode> getNodes() {
        return nodes;
    }
}
