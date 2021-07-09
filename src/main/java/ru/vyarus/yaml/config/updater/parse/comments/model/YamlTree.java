package ru.vyarus.yaml.config.updater.parse.comments.model;

import ru.vyarus.yaml.config.updater.parse.common.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.common.model.TreeRoot;

import java.util.List;

/**
 * Yaml tree built by comments parser.
 *
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlTree extends TreeRoot<YamlNode> {

    public YamlTree(final List<YamlNode> nodes) {
        super(nodes);
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
    private void renderNode(final YamlNode node, final StringBuilder out, final boolean noPadding) {
        final String padding = noPadding ? "" : TreeStringUtils.whitespace(node.getPadding());

        if (!node.getTopComment().isEmpty()) {
            out.append(padding).append("# comment");
            if (node.getTopComment().size() > 1) {
                out.append(" ").append(node.getTopComment().size()).append(" lines");
            }
            out.append("\n");

            if (node.isCommentOnly()) {
                return;
            }
        }

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
                out.append("'").append(node.getValue().get(0).trim()).append("'");
            } else {
                out.append("value ").append(node.getValue().size()).append(" lines");
            }
        }
        // for objects in list node virtual node created, splitting line into two objects (if no empty dash used)
        boolean mergeLines = node.isListItem() && node.isListItemWithProperty();
        if (!mergeLines) {
            out.append("\n");
        }

        for (YamlNode child : node.getChildren()) {
            // render dash and first item property as single line (when mergeLines = true)
            renderNode(child, out, mergeLines);
            mergeLines = false;
        }
    }
}
