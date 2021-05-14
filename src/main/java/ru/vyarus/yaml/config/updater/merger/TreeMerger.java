package ru.vyarus.yaml.config.updater.merger;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.model.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
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
        if (!from.hasChildren()) {
            // nothing to sync - current children subtree remains
            return;
        }

        // special logic for list values
        if (processList(node, from)) {
            return;
        }

        // updating file structure taken and only existing nodes replaced by current values
        // nodes not found in new config would be also inserted

        Map<String, YamlNode> newProps = from.getProps();

        List<YamlNode> updated = new ArrayList<>(from.getChildren());

        // current file paddings must be unified with updating file or the resulting file become invalid
        int padding = from.getChildren().get(0).getPadding();
        // previous node index
        int prevNodeIdx = -1;

        for (int i = 0; i < node.getChildren().size(); i++) {
            YamlNode curr = node.getChildren().get(i);

            // update old node's padding
            shiftNode(curr, padding - curr.getPadding());

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

            if (prevNodeIdx < 0) {
                // first node will also go first
                updated.add(0, curr);
            } else {
                // insert it after old previous element (in the new list)
                updated.add(++prevNodeIdx, curr);
            }
        }

        node.getChildren().clear();
        node.getChildren().addAll(updated);
    }

    private static boolean processList(TreeNode<YamlNode> node, TreeNode<YamlNode> from) {
        if (node.containsList()) {
            YamlNode cur = (YamlNode) node;
            YamlNode upd = (YamlNode) from;

            // first of all sync paddings (no matter if list is a scalar and would not be updated)
            int pad = upd.getChildren().get(0).getPadding();
            for(YamlNode child: cur.getChildren()) {
                // important to shift list node itself before continuing (otherwise subtree could be shifted)
                shiftNode(child, pad - child.getPadding());
            }

            // synchronizing only object lists
            if (cur.getChildren().get(0).isProperty()) {
                // ASSUMPTION: first list item object property is an identity property

                for (YamlNode child : cur.getChildren()) {
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
            return true;
        }
        return false;
    }

    private static void shiftNode(YamlNode node, int shift) {
        boolean increase = shift > 0;
        if (shift != 0) {
            if (node.getValue().size() > 1) {
                // important to shift multiline values (otherwise value may be flowed)

                // first value line is a part of property declaration
                List<String> res = new ArrayList<>();
                res.add(node.getValue().get(0));
                for (int j = 1; j < node.getValue().size(); j++) {
                    String line = node.getValue().get(j);

                    // skip blank lines
                    if (line.trim().isEmpty()) {
                        res.add(line);
                        continue;
                    }

                    if (increase) {
                        // increase padding
                        final char[] space = new char[shift];
                        Arrays.fill(space, ' ');
                        line = String.valueOf(space) + line;
                    } else {
                        // reduce padding (cut off whitespace)
                        line = line.substring(-shift);
                    }
                    res.add(line);
                }
                node.setValue(res);
            }
            if (node.hasComment()) {
                // shifting comment
                List<String> cmt = new ArrayList<>();
                for (String line: node.getTopComment()) {
                    // skip blank lines
                    if (line.trim().isEmpty()) {
                        cmt.add(line);
                        continue;
                    }

                    if (increase) {
                        // increase padding
                        final char[] space = new char[shift];
                        Arrays.fill(space, ' ');
                        line = String.valueOf(space) + line;
                    } else {
                        // reduce padding (cut off whitespace)
                        int cmtStart = line.indexOf('#');
                        // shift left, but only whitespace before comment
                        line = line.substring(Math.min(-shift, cmtStart));
                    }
                    cmt.add(line);
                }
                node.getTopComment().clear();
                node.getTopComment().addAll(cmt);
            }
            node.setKeyPadding(node.getKeyPadding() + shift);
            node.setPadding(node.getPadding() + shift);

            // important to shift entire subtree (otherwise list position could be flowed)
            for (YamlNode child : node.getChildren()) {
                shiftNode(child, shift);
            }
        }
    }
}
