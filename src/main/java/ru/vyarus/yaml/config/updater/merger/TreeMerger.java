package ru.vyarus.yaml.config.updater.merger;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.model.TreeNode;
import ru.vyarus.yaml.config.updater.parse.model.YamlLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
public class TreeMerger {

    public static void merge(YamlTree node, YamlTree from) {
        mergeLevel(node, from);
    }

    private static void mergeLevel(TreeNode<YamlNode> node, TreeNode<YamlNode> from) {
        if (node instanceof YamlLine && ((YamlNode) node).isListValue()) {
            // todo support lists merging and ESPECIALLY complex list structures
            return;
        }

        Map<String, YamlNode> newProps = from.getProps();
        List<String> processed = new ArrayList<>();
        for(YamlNode child: node.getChildren()) {
            YamlNode update = newProps.get(child.getKey());
            if (update != null) {
                mergeLevel(child, update);
                processed.add(child.getKey());
            }
        }

        // adding all new nodes ar the end
        // todo later implement correct in-place addition
        for (String key: newProps.keySet()) {
            if (!processed.contains(key)) {
                node.getChildren().add(newProps.get(key));
            }
        }
    }
}
