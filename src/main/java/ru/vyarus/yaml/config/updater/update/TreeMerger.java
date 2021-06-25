package ru.vyarus.yaml.config.updater.update;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.common.model.TreeNode;

import java.util.*;

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
public final class TreeMerger {

    private TreeMerger() {
    }

    public static void merge(final YamlTree node, final YamlTree from) {
        mergeLevel(node, from);
    }

    private static void mergeLevel(final TreeNode<YamlNode> node, final TreeNode<YamlNode> from) {
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

        final Map<String, YamlNode> newProps = from.getRootProperties();

        final List<YamlNode> updated = new ArrayList<>(from.getChildren());

        // current file paddings must be unified with updating file or the resulting file become invalid
        final int padding = from.getChildren().get(0).getPadding();
        // previous node index
        int prevNodeIdx = -1;

        for (int i = 0; i < node.getChildren().size(); i++) {
            final YamlNode curr = node.getChildren().get(i);

            // update old node's padding
            shiftNode(curr, padding - curr.getPadding());

            final String key = curr.getKey();
            if (curr.isProperty() && newProps.containsKey(key)) {
                // replace new node with old node
                final int idx = updated.indexOf(newProps.get(key));
                final YamlNode newnode = updated.remove(idx);
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
        // node containing list items (node itself is not a list item)
        if (node.containsList()) {
            final YamlNode cur = (YamlNode) node;
            final YamlNode upd = (YamlNode) from;

            // nothing to merge
            if (!upd.hasChildren()) {
                return true;
            }

            // first of all sync paddings (no matter if list is a scalar and would not be updated)
            final int pad = upd.getChildren().get(0).getPadding();
            for (YamlNode child : cur.getChildren()) {
                // important to shift list node itself before continuing (otherwise subtree could be shifted)
                shiftNode(child, pad - child.getPadding());
            }

            // Processing required only for lists with object nodes (assuming new properties might be added to object)
            // For both scalar and object lists new list items are not added

            // use wrapper objects to simplify matching
            final List<YamlNode> curList = cur.getChildren();
            final List<YamlNode> updList = new ArrayList<>(upd.getChildren());

            // all items should be unified with the new file structure (e.g. empty dash -> normal dash)
            // remembering target structure
            final boolean targetEmptyDash = updList.get(0).isEmptyDash();

            for (YamlNode item : curList) {
                // nothing to sync in scalar items
                if (!item.isObjectListItem()) {
                    continue;
                }

                final YamlNode match = ListMatcher.match(item, updList);
                if (match != null) {
                    // actual items merge (padding is already synced so no additional shift will appear)
                    mergeLevel(item, match);

                    // avoid one node matches for multiple nodes
                    updList.remove(match);
                }

                if (updList.isEmpty()) {
                    break;
                }
            }

            // recover merged items structure
            for (YamlNode item : curList) {
                // re-mapping new nodes
                item.setRoot(cur);
                if (!cur.getChildren().contains(item)) {
                    cur.getChildren().add(item);
                }
                item.getChildren().forEach(yamlNode -> yamlNode.setRoot(item));

                if (item.isObjectListItem()) {
                    // list style could change (empty dash -> single line or reverse)
                    item.setListItemWithProperty(!targetEmptyDash);

                    final YamlNode firstItemLine = item.getChildren().get(0);
                    if (item.isListItemWithProperty() && firstItemLine.hasComment()) {
                        // if first item contains comment need to move it before dash
                        item.getTopComment().addAll(firstItemLine.getTopComment());
                        firstItemLine.getTopComment().clear();
                    }
                }
            }

            return true;
        }
        return false;
    }

    private static void shiftNode(final YamlNode node, final int shift) {
        final boolean increase = shift > 0;
        if (shift != 0) {
            if (node.getValue().size() > 1) {
                // important to shift multiline values (otherwise value may be flowed)

                // first value line is a part of property declaration
                final List<String> res = new ArrayList<>();
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
                        line = TreeStringUtils.shiftRight(line, shift);
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
                final List<String> cmt = new ArrayList<>();
                for (String ln : node.getTopComment()) {
                    String line = ln;
                    // skip blank lines
                    if (line.trim().isEmpty()) {
                        cmt.add(line);
                        continue;
                    }

                    if (increase) {
                        // increase padding
                        line = TreeStringUtils.shiftRight(line, shift);
                    } else {
                        // reduce padding (cut off whitespace)
                        final int cmtStart = line.indexOf('#');
                        // shift left, but only whitespace before comment
                        line = line.substring(Math.min(-shift, cmtStart));
                    }
                    cmt.add(line);
                }
                node.getTopComment().clear();
                node.getTopComment().addAll(cmt);
            }
            node.setPadding(node.getPadding() + shift);

            // important to shift entire subtree (otherwise list position could be flowed)
            for (YamlNode child : node.getChildren()) {
                shiftNode(child, shift);
            }
        }
    }
}
