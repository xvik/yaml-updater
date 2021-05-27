package ru.vyarus.yaml.config.updater.merger;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.comments.util.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.model.TreeNode;

import java.util.*;

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
public class TreeMerger {

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

        final Map<String, YamlNode> newProps = from.getProps();

        final List<YamlNode> updated = new ArrayList<>(from.getChildren());

        // current file paddings must be unified with updating file or the resulting file become invalid
        final int padding = from.getChildren().get(0).getPadding();
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
        // node containing list items (node itself is not a list item)
        if (node.containsList()) {
            YamlNode cur = (YamlNode) node;
            YamlNode upd = (YamlNode) from;

            // nothing to merge
            if (!upd.hasChildren()) {
                return true;
            }

            // first of all sync paddings (no matter if list is a scalar and would not be updated)
            int pad = upd.getChildren().get(0).getPadding();
            for (YamlNode child : cur.getChildren()) {
                // important to shift list node itself before continuing (otherwise subtree could be shifted)
                shiftNode(child, pad - child.getPadding());
            }

            // Processing required only for lists with object nodes (assuming new properties might be added to object)
            // For both scalar and object lists new list items are not added
            // List structure might be simply shifted object (first property with dash and other item properties
            // are children) or dash line might be empty, in this case first node is empty and all item props
            // specified as children (keeping declaration structure is important for comments recovery)

            // flat lists required for objects comparison
            final List<YamlListNode> curList = flattenListItems(cur);
            final List<YamlListNode> updList = flattenListItems(upd);

            // all items should be unified with the new file structure (e.g. empty dash -> normal dash)
            // remembering target structure
            boolean targetEmptyDash = updList.get(0).emptyDash;

            for (YamlListNode item : curList) {
                // nothing to sync in scalar items
                if (!item.object) {
                    continue;
                }

                final YamlListNode match = findMatch(item, updList);
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
            for (YamlListNode item : curList) {
                YamlNode root;
                if (targetEmptyDash) {
                    if (item.emptyDash) {
                        // empty -- empty (no change)
                        root = item.identity;
                        item.identity.getChildren().clear();
                    } else {
                        // prop -- empty (new node required)
                        root = new YamlNode(cur, pad, item.identity.getLineNum());
                        root.setValue(new ArrayList<>(Collections.singletonList("")));
                    }
                } else {
                    // no matter how it was, always use first prop as root
                    root = item.getChildren().remove(0);
                }
                root.setRoot(cur);
                root.setPadding(pad);
                root.setListValue(true);
                root.getChildren().addAll(item.getChildren());
                root.getChildren().forEach(yamlNode -> yamlNode.setRoot(root));
                // re-attach item to list node
                if (!cur.getChildren().contains(root)) {
                    cur.getChildren().add(root);
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
                List<String> cmt = new ArrayList<>();
                for (String line : node.getTopComment()) {
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

    private static List<YamlListNode> flattenListItems(final YamlNode node) {
        // required to unify various list representations (especially id current and updating lists use different
        // schemes)
        final List<YamlListNode> res = new ArrayList<>();
        for (YamlNode item : node.getChildren()) {
            final YamlListNode child = new YamlListNode(item);
            child.getChildren().addAll(item.getChildren());
            if (!child.emptyDash) {
                // first shifted property
                child.getChildren().add(0, item);
                // have to damage structure for correct merging
                item.getChildren().clear();

                // node should not remain list value because order of props could change
                item.setListValue(false);
                item.setPadding(item.getKeyPadding());
            }
            if (child.hasChildren() && child.getChildren().get(0).isProperty()) {
                child.object = true;
            }
            res.add(child);
        }
        // processed items would be re-added
        node.getChildren().clear();
        return res;
    }

    private static YamlListNode findMatch(final YamlListNode node, final List<YamlListNode> list) {
        final List<YamlListNode> cand = new ArrayList<>(list);
        // reset counters for new item matching
        cand.forEach(yamlListNode -> yamlListNode.propsMatched = 0);

        // using as much properties as required to find unique match
        for (YamlNode prop : node.getChildren()) {
            if (!prop.hasValue()) {
                continue;
            }
            final Iterator<YamlListNode> it = cand.iterator();
            // searching matched item by one prop (from previously selected nodes)
            while (it.hasNext()) {
                YamlListNode cnd = it.next();
                boolean match = false;
                boolean propFound = false;
                for (YamlNode uprop : cnd.getChildren()) {
                    if (prop.getKey().equals(uprop.getKey())) {
                        propFound = true;
                        if (prop.getValueIdentity().equals(uprop.getValueIdentity())) {
                            match = true;
                            cnd.propsMatched++;
                        }
                        break;
                    }
                }
                // avoid removing items where tested property was missing (maybe other props would match)
                if (propFound && !match) {
                    it.remove();
                }
            }
            if (cand.isEmpty()) {
                // nothing matched or exactly one match
                break;
            }
        }

        // filter candidates without any match (to avoid false matching for totally different lists)
        cand.removeIf(yamlListNode -> yamlListNode.propsMatched == 0);

        // search for EXACT match
        return cand.size() == 1 ? cand.get(0) : null;
    }

    private static class YamlListNode extends YamlNode {
        public final YamlNode identity;
        // empty dash line or first property just after dash
        public boolean emptyDash;
        // object item or scalar
        public boolean object;

        // used during items matching to count how many properties match
        public int propsMatched;

        public YamlListNode(final YamlNode item) {
            // line number is unique identity for list item
            super(null, item.getPadding(), item.getLineNum());
            this.identity = item;
            this.emptyDash = !item.hasValue() && item.hasChildren();
        }

        @Override
        public String toString() {
            return "(" + (object ? "object " + getChildren().size() : "scalar") + " | " + (emptyDash ? "empty dash" : "inline") + ") "
                    + identity.getLineNum() + ": " + identity;
        }
    }
}
