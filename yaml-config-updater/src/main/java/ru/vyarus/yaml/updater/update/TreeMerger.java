package ru.vyarus.yaml.updater.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.updater.parse.common.model.TreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Merges commented models. Rules:
 * - All yaml nodes presented in current config will remain, but comments might be updated (if matching node found
 * in update file).
 * - All new properties copied from update file.
 * - Update file's properties order used (so if in current and update file the same properties would be used,
 * but order changed - update file order would be applied).
 * - Properties padding taken from update file. For example, if in current file properties were shifted with two spaced
 * and in update file with 4 then all properties would be shifted according to update file (even if no new properties
 * applied). Shift appear on subtree level (where subtrees could be matched) so if there are subtrees in old file
 * not present in new one - old paddings will remain there (no target to align by).
 * - Lists are not merged. But if list contain object items, such items are updated (new properties added).
 * Items matched by property values.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
@SuppressWarnings("PMD.InefficientEmptyStringCheck")
public final class TreeMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(TreeMerger.class);

    private TreeMerger() {
    }

    /**
     * Merge commented models.
     *
     * @param node old file
     * @param from new file
     */
    public static void merge(final CmtTree node, final CmtTree from) {
        mergeLevel(node, from);

        // if both files contain trailing comment, they both would merge
        // (trailing comments impossible on deeper levels)
        int trailing = 0;
        for (CmtNode child : node.getChildren()) {
            if (child.isCommentOnly()) {
                trailing++;
            }
        }
        // could only be 2
        if (trailing > 1) {
            // new trailing comment will go last
            node.getChildren().remove(node.getChildren().size() - 2);
            LOGGER.debug("Trailing comment replaced");
        }
    }

    private static void mergeLevel(final TreeNode<CmtNode> node, final TreeNode<CmtNode> from) {
        // nothing to sync case (current children subtree remains) and special logic for list values
        if (!from.hasChildren() || processList(node, from)) {
            return;
        }

        // updating file structure based on updating file nodes, replacing values present in current file
        // nodes not found in new config would be inserted (in case of multiple nodes in the same order)

        final Map<String, CmtNode> newProps = from.getRootProperties();

        final List<CmtNode> updated = new ArrayList<>(from.getChildren());
        // mark all nodes as added by default (existing nodes would be replaced)
        updated.forEach(cmtNode -> cmtNode.setAddedNode(true));

        // current file paddings must be unified with updating file or the resulting file become invalid
        final int padding = from.getChildren().get(0).getPadding();
        // previous node index
        int prevNodeIdx = -1;

        for (int i = 0; i < node.getChildren().size(); i++) {
            final CmtNode curr = node.getChildren().get(i);

            // update old node's padding
            shiftNode(curr, padding - curr.getPadding());

            final String key = curr.getKey();
            if (curr.isProperty() && newProps.containsKey(key)) {
                // replace new node with old node
                final int idx = updated.indexOf(newProps.get(key));
                final CmtNode newnode = updated.remove(idx);
                updated.add(idx, curr);

                // copy comment from new node (it might be updated and contain more actual instructions)
                if (newnode.hasComment()) {
                    curr.getTopComment().clear();
                    curr.getTopComment().addAll(newnode.getTopComment());
                }

                // property style could change (quoted to unquoted or the opposite)
                curr.setSourceKey(newnode.getSourceKey());

                // sync entire tree
                mergeLevel(curr, newnode);

                prevNodeIdx = idx;
                continue;
            }

            // current node not found in new tree: trying to find a good place for insertion using previous context

            if (prevNodeIdx < 0) {
                // first node will also go first
                updated.add(0, curr);
                // if multiple properties from current file absent in new file, they must go in the same order
                prevNodeIdx = 0;
            } else {
                // insert it after old previous element (in the new list)
                updated.add(++prevNodeIdx, curr);
            }
        }

        node.getChildren().clear();
        node.addAll(updated);
    }

    private static boolean processList(final TreeNode<CmtNode> node, final TreeNode<CmtNode> from) {
        final boolean isList = node.hasListValue();
        // node containing list items (node itself is not a list item)
        // and target node contains children (nothing to merge otherwise)
        if (isList && from.hasChildren()) {
            final CmtNode cur = (CmtNode) node;
            final CmtNode upd = (CmtNode) from;

            // first of all, sync paddings (no matter if list is a scalar and would not be updated)
            final int pad = upd.getChildren().get(0).getPadding();
            for (CmtNode child : cur.getChildren()) {
                // important to shift list node itself before continuing (otherwise subtree could be shifted)
                shiftNode(child, pad - child.getPadding());
            }

            // Processing required only for lists with object nodes (assuming new properties might be added to object)
            // For both scalar and object lists new list items are not added

            final List<CmtNode> updList = new ArrayList<>(upd.getChildren());

            // all items should be unified with the new file structure (e.g. empty dash -> normal dash)
            // remembering target structure
            final boolean targetEmptyDash = updList.get(0).isEmptyDash();

            for (CmtNode item : cur.getChildren()) {
                // nothing to sync in scalar items
                if (!item.isObjectListItem()) {
                    continue;
                }

                final CmtNode match = ListMatcher.match(item, updList);
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
            updateListStructure(cur, targetEmptyDash);
        }
        return isList;
    }

    private static void updateListStructure(final CmtNode cur, final boolean targetEmptyDash) {
        for (CmtNode item : cur.getChildren()) {
            item.getChildren().forEach(yamlNode -> yamlNode.setRoot(item));

            if (item.isObjectListItem()) {
                // list style could change (empty dash -> single line or reverse)
                item.setListItemWithProperty(!targetEmptyDash);

                final CmtNode firstItemLine = item.getChildren().get(0);
                if (item.isListItemWithProperty() && firstItemLine.hasComment()) {
                    // if first item contains comment need to move it before dash
                    item.getTopComment().addAll(firstItemLine.getTopComment());
                    firstItemLine.getTopComment().clear();
                }
            }
        }
    }

    private static void shiftNode(final CmtNode node, final int shift) {
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
            shiftComment(node, shift, increase);
            node.setPadding(node.getPadding() + shift);

            // important to shift entire subtree (otherwise list position could be flowed)
            for (CmtNode child : node.getChildren()) {
                shiftNode(child, shift);
            }
        }
    }

    private static void shiftComment(final CmtNode node, final int shift, final boolean increase) {
        if (node.hasComment()) {
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
    }
}
