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
        // todo trees must be unified by padding first!
        mergeLevel(node, from);
    }

    private static void mergeLevel(TreeNode<YamlNode> node, TreeNode<YamlNode> from) {
        if (from.getChildren().isEmpty()) {
            // nothing to sync - current children subtree remains
            return;
        }

        // special logic for list values
        if (node instanceof YamlLine && !node.getChildren().isEmpty() && node.getChildren().get(0).isListValue()) {
            YamlNode cur = (YamlNode) node;
            YamlNode upd = (YamlNode) from;
            // looking only object lists
            if (cur.getChildren().get(0).isProperty() && !upd.getChildren().isEmpty()) {
                // ASSUMPTION: first list item object property is an identity property

                for(YamlNode child: cur.getChildren()) {
                    // no way to track node
                    if (!child.hasValue()) {
                        continue;
                    }

                    String key = child.getKey();
                    String val = child.getFirstLineValue();

                    // important to find only one match in target list
                    List<YamlNode> cand = new ArrayList<>();
                    for (YamlNode up : upd.getChildren()) {
                        if (key.equals(up.getKey()) && val.equals(up.getFirstLineValue())) {
                            cand.add(up);
                        }
                    }

                    if (cand.size() != 1) {
                        // failed to find EXACT matching value
                        continue;
                    }

                    // merge object subtree
                    mergeLevel(child, cand.get(0));
                }
            }
            return;
        }

        // updating file structure taken and only existing nodes replaced by current values
        // nodes not found in new config would be also inserted

        Map<String, YamlNode> newProps = from.getProps();

        List<YamlNode> updated = new ArrayList<>(from.getChildren());

        int prevNodeIdx = 0;

        for (int i = 0; i < node.getChildren().size(); i++) {
            YamlNode curr = node.getChildren().get(i);
            final String key = curr.getKey();
            if (curr.isProperty() && newProps.containsKey(key)) {
                // replace new node with old node
                int idx = updated.indexOf(newProps.get(key));
                YamlNode newnode = updated.remove(idx);
                updated.add(idx, curr);

                // copy comment from new node (it might be updated and contain more actual instructions)
                if (newnode.hasComment()) {
                    curr.getTopComment().clear();
                    curr.getTopComment().addAll(newnode.getTopComment());
                }

                // sync entire tree
                mergeLevel(curr, newnode);

                prevNodeIdx = idx;
                continue;
            }

            // current node not found in new tree: trying to find a good place for insertion using previous context

            if (prevNodeIdx == 0) {
                // first node will also go first
                updated.add(0, curr);
            } else {
                // insert it after previous element
                updated.add(++prevNodeIdx, curr);
            }
        }

        node.getChildren().clear();
        node.getChildren().addAll(updated);
    }
}
