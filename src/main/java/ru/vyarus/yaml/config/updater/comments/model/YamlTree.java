package ru.vyarus.yaml.config.updater.comments.model;

import java.util.Arrays;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlTree {

    private final List<YamlNode> nodes;

    public YamlTree(final List<YamlNode> nodes) {
        this.nodes = nodes;
    }

    public List<YamlNode> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        // to string shows structural view to quickly identify parsing errors
        final StringBuilder out = new StringBuilder();

        if (nodes == null || nodes.isEmpty()) {
            out.append("<empty>");
        } else {
            nodes.forEach(node -> renderNode(node, out));
        }

        return out.toString();
    }

    private void renderNode(final YamlNode node, final StringBuilder out) {
        String padding = "";
        if (node.getPadding() > 0) {
            final char[] space = new char[node.getPadding()];
            Arrays.fill(space, ' ');
            padding = String.valueOf(space);
        }

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
        if (node.isListValue()) {
            out.append("- ");
        }
        if (node.getName() != null) {
            out.append(node.getName()).append(": ");
        }
        if (!node.getValue().isEmpty()) {
            if (node.getValue().size() == 1) {
                // for batter navigation show simple values
                out.append(node.getValue().get(0));
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
