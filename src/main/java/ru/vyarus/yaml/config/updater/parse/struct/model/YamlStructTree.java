package ru.vyarus.yaml.config.updater.parse.struct.model;

import java.util.Arrays;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
public class YamlStructTree {

    private final List<YamlStruct> nodes;

    public YamlStructTree(final List<YamlStruct> nodes) {
        this.nodes = nodes;
    }

    public List<YamlStruct> getNodes() {
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

    private void renderNode(final YamlStruct node, final StringBuilder out) {
        String padding = "";
        if (node.getPadding() > 0) {
            final char[] space = new char[node.getPadding()];
            Arrays.fill(space, ' ');
            padding = String.valueOf(space);
        }

        out.append(padding);
        if (node.isListValue()) {
            out.append("- ");
        }
        if (node.getKey() != null) {
            out.append(node.getKey()).append(": ");
        }
        if (node.getValue() != null) {
            // identify multiline values (and avoid visual ambiguity)
            String val = node.getValue().replace("\n", "\\n");
            if (val.length() > 80) {
                val = val.substring(0, 80) + "...";
            }
            out.append(val);
        }
        out.append("\n");

        for (YamlStruct child : node.getChildren()) {
            renderNode(child, out);
        }
    }
}
