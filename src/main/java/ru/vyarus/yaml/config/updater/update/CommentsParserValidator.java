package ru.vyarus.yaml.config.updater.update;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.config.updater.parse.common.model.YamlLine;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStruct;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 18.05.2021
 */
public final class CommentsParserValidator {

    private CommentsParserValidator() {
    }

    public static void validate(final YamlTree comments, final YamlStructTree struct) {
        validateSubtrees(comments, struct);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private static void validateSubtrees(final TreeNode<YamlNode> comments, final TreeNode<YamlStruct> struct) {
        final List<YamlNode> children = comments.getChildren();
        final List<YamlStruct> childrenStruct = struct.getChildren();
        if (children.size() < childrenStruct.size()) {
            throw new IllegalStateException("Comments parser validation problem on line " + comments.getLineNum()
                    + ": " + children.size() + " child nodes found but should be at least " + childrenStruct.size()
                    + "(this is parser a bug, please report it!)\n" + debugTees(comments, struct));
        }

        final Iterator<YamlNode> cmtIt = children.iterator();
        final Iterator<YamlStruct> strIt = childrenStruct.iterator();
        while (cmtIt.hasNext()) {
            final YamlNode line = cmtIt.next();

            if (line.isCommented() || line.isCommentOnly()) {
                // structure parser can't see commented lines and so can't validate correctness
                continue;
            }

            if (!strIt.hasNext()) {
                throw new IllegalStateException("Comments parser validation problem on line "
                        + line.getLineNum() + ": line should not exist (this is a parser bug, please report it!)\n"
                        + debugTees(comments, struct));
            }
            final YamlStruct match = strIt.next();

            if (line.isProperty()) {
                if (!line.getKey().equals(match.getKey())) {
                    throw new IllegalStateException("Comments parser validation problem on line "
                            + line.getLineNum()
                            + ": line should be different: " + match + " (this is a parser bug, please report it!)\n"
                            + debugTees(comments, struct));
                }

                // store correctly parsed value (without comments) for precise list items matching
                line.setParsedValue(match.getValue());
            }

            // validate subtree (even for non properties because structures must be equal)
            validateSubtrees(line, match);
        }
    }

    private static String debugTees(final TreeNode<YamlNode> comments, final TreeNode<YamlStruct> struct) {
        final ReportLines cmtTree = debugTree(comments);
        final List<String> res = new ArrayList<>();
        if (struct != null) {
            res.add("Comments parser subtree:");
        }
        res.addAll(cmtTree.getLines());

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
            for (int j = 1; j < strTree.getLines().size() + 1; j++) {
                res.add(merge[j] + strTree.getLines().get(j - 1));
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

    @SuppressWarnings("checkstyle:VisibilityModifier")
    private static class ReportLines {
        List<String> lines = new ArrayList<>();
        int length;

        public void add(final String line) {
            length = Math.max(length, line.length());
            lines.add(line);
        }

        public List<String> getLines() {
            return lines;
        }

        public int getLength() {
            return length;
        }
    }
}
