package ru.vyarus.yaml.updater.update;

import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.updater.parse.common.model.YamlLine;
import ru.vyarus.yaml.updater.parse.struct.model.StructNode;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Comments parse is a very simple yaml parser and to make sure it works correctly comparing parsed structure with
 * snakeyaml parse result (parsed trees are unified to represent equal trees).
 * <p>
 * Comments parser and snakeyaml models are unified to get equally trees (easier for comparison).
 * <p>
 * During comparison, property values parsed by snakeyaml are assigned in comments tree. This is required for
 * list items matching logic accuracy (otherwise different comments near values could prevent values matching).
 *
 * @author Vyacheslav Rusakov
 * @since 18.05.2021
 */
public final class CommentsParserValidator {

    private CommentsParserValidator() {
    }

    /**
     * Compare that parsed trees are structurally the same to verify comments parser correctness (assuming snakeyaml
     * works perfectly).
     *
     * @param comments comments parser result
     * @param struct snakeyaml result
     */
    public static void validate(final CmtTree comments, final StructTree struct) {
        validateSubtrees(comments, struct);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private static void validateSubtrees(final TreeNode<CmtNode> comments, final TreeNode<StructNode> struct) {
        final List<CmtNode> children = comments.getChildren();
        final List<StructNode> childrenStruct = struct.getChildren();
        if (children.size() < childrenStruct.size()) {
            throw new IllegalStateException("Comments parser validation problem on line " + comments.getLineNum()
                    + ": " + children.size() + " child nodes found but should be at least " + childrenStruct.size()
                    + " (this is a parser bug, please report it!)\n" + debugTees(comments, struct));
        }

        final Iterator<CmtNode> cmtIt = children.iterator();
        final Iterator<StructNode> strIt = childrenStruct.iterator();
        while (cmtIt.hasNext()) {
            final CmtNode line = cmtIt.next();

            if (line.isCommentOnly()) {
                // "fake" node in comments parser for preserving trailing comment
                continue;
            }

            if (!strIt.hasNext()) {
                throw new IllegalStateException("Comments parser validation problem on line "
                        + line.getLineNum() + ": line should not exist (this is a parser bug, please report it!)\n"
                        + debugTees(line, null));
            }
            final StructNode match = strIt.next();

            if (line.isProperty()) {
                if (!line.getKey().equals(match.getKey())) {
                    throw new IllegalStateException("Comments parser validation problem on line "
                            + line.getLineNum()
                            + ": line should be different: \"" + match + "\" (this is a parser bug, please report it!)\n"
                            + debugTees(comments, struct));
                }

                // store correctly parsed value (without comments) for precise list items matching
                line.setParsedValue(match.getValue());
            }

            // validate subtree (even for non properties because structures must be equal)
            validateSubtrees(line, match);
        }
    }

    /**
     * Prints comments model tree nearby snakeyaml model tree to visually see the difference.
     *
     * @param comments comments subtree
     * @param struct snakeyaml subtree
     * @return rendered trees string
     */
    private static String debugTees(final TreeNode<CmtNode> comments, final TreeNode<StructNode> struct) {
        final ReportLines cmtTree = debugTree(comments);
        final List<String> res = new ArrayList<>();
        if (struct != null) {
            res.add("Comments parser subtree:");
        }
        res.addAll(cmtTree.lines);

        if (struct != null) {
            final ReportLines strTree = debugTree(struct);
            final String[] merge = new String[Math.max(res.size(), strTree.lines.size() + 1)];
            final int pad = Math.max(cmtTree.length, res.get(0).length()) + 4;

            // align lines
            int i = 0;
            for (String line : res) {
                merge[i++] = TreeStringUtils.fillTo(line, pad);
            }
            // align lines count
            while (i < merge.length) {
                merge[i++] = TreeStringUtils.whitespace(pad);
            }

            res.clear();
            res.add(merge[0] + "Structure parser subtree:");
            for (int j = 1; j < strTree.lines.size() + 1; j++) {
                res.add(merge[j] + strTree.lines.get(j - 1));
            }
        }

        // shift result to fit exception output
        final StringBuilder collect = new StringBuilder();
        for (String line : res) {
            collect.append(TreeStringUtils.shiftRight(line, 6)).append('\n');
        }
        return collect.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends YamlLine<T>> ReportLines debugTree(final TreeNode<T> node) {
        final ReportLines lines = new ReportLines();
        if (node instanceof YamlLine) {
            debugTreeLeaf(lines, (T) node, 0);
        } else {
            // for tree root print entire tree
            for(T child: node.getChildren()) {
                debugTreeLeaf(lines, child, 0);
            }
        }
        return lines;
    }

    private static <T extends YamlLine<T>> void debugTreeLeaf(
            final ReportLines lines,
            final T line,
            final int padding) {
        final String ln = String.format("%4s| ", line.getLineNum())
                + TreeStringUtils.shiftRight(line.toString(), padding);
        lines.add(ln);
        for (T child : line.getChildren()) {
            debugTreeLeaf(lines, child, padding + 2);
        }
    }

    @SuppressWarnings({"checkstyle:VisibilityModifier", "PMD.DefaultPackage"})
    private static class ReportLines {
        List<String> lines = new ArrayList<>();
        int length;

        public void add(final String line) {
            length = Math.max(length, line.length());
            lines.add(line);
        }
    }
}
