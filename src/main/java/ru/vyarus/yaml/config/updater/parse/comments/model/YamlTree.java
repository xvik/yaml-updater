package ru.vyarus.yaml.config.updater.parse.comments.model;

import ru.vyarus.yaml.config.updater.parse.comments.util.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.model.TreeRoot;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlTree extends TreeRoot<YamlNode> {

    public YamlTree(final List<YamlNode> nodes) {
        super(null);
        getChildren().addAll(nodes);
    }

    @Override
    public String toString() {
        // to string shows structural view to quickly identify parsing errors
        final StringBuilder out = new StringBuilder();

        if (getChildren().isEmpty()) {
            out.append("<empty>");
        } else {
            getChildren().forEach(node -> renderNode(node, out));
        }

        return out.toString();
    }

    private void renderNode(final YamlNode node, final StringBuilder out) {
        String padding = TreeStringUtils.whitespace(node.getPadding());

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
        out.append("\n");

        for (YamlNode child : node.getChildren()) {
            renderNode(child, out);
        }
    }
}
