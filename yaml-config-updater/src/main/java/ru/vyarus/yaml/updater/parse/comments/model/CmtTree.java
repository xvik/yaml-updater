package ru.vyarus.yaml.updater.parse.comments.model;

import ru.vyarus.yaml.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.updater.parse.common.model.TreeRoot;

import java.util.List;

/**
 * Yaml tree built by comments parser.
 *
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class CmtTree extends TreeRoot<CmtNode> {

    public CmtTree(final List<CmtNode> nodes, final int linesCnt) {
        super(nodes, linesCnt);
    }

    @Override
    public String toString() {
        // to string shows structural view to quickly identify parsing errors
        final StringBuilder out = new StringBuilder();

        if (getChildren().isEmpty()) {
            out.append("<empty>");
        } else {
            getChildren().forEach(node -> renderNode(node, out, false));
        }

        return out.toString();
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private void renderNode(final CmtNode node, final StringBuilder out, final boolean noPadding) {
        final String padding = noPadding ? "" : TreeStringUtils.whitespace(node.getPadding());

        if (!node.getTopComment().isEmpty()) {
            out.append(padding).append("# comment");
            if (node.getTopComment().size() > 1) {
                out.append(' ').append(node.getTopComment().size()).append(" lines");
            }
            out.append('\n');

            if (node.isCommentOnly()) {
                return;
            }
        }

        renderValue(node, out, padding);
        // for objects in list node virtual node created, splitting line into two objects (if no empty dash used)
        boolean mergeLines = node.isListItem() && node.isListItemWithProperty();
        if (!mergeLines) {
            out.append('\n');
        }

        for (CmtNode child : node.getChildren()) {
            // render dash and first item property as single line (when mergeLines = true)
            renderNode(child, out, mergeLines);
            mergeLines = false;
        }
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private void renderValue(final CmtNode node, final StringBuilder out, final String padding) {
        out.append(padding);
        if (node.isListItem()) {
            out.append("- ");
        }
        if (node.getKey() != null) {
            out.append(node.getKey()).append(": ");
        }
        if (!node.getValue().isEmpty()) {
            if (node.getValue().size() == 1) {
                // for batter navigation show simple values
                out.append('\'').append(node.getValue().get(0).trim()).append('\'');
            } else {
                out.append("value ").append(node.getValue().size()).append(" lines");
            }
        }
    }
}
