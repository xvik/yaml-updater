package ru.vyarus.yaml.config.updater.parse.struct.model;

import ru.vyarus.yaml.config.updater.parse.comments.util.TreeStringUtils;
import ru.vyarus.yaml.config.updater.parse.model.TreeRoot;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
public class YamlStructTree extends TreeRoot<YamlStruct> {

    public YamlStructTree(final List<YamlStruct> nodes) {
        super(null);
        this.getChildren().addAll(nodes);
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

    private void renderNode(final YamlStruct node, final StringBuilder out) {
        out.append(TreeStringUtils.whitespace(node.getPadding()));
        if (node.isListItem()) {
            out.append("- ");
        }
        if (node.getKey() != null) {
            out.append(node.getKey()).append(": ");
        }
        if (node.getValue() != null) {
            // identify multiline values (and avoid visual ambiguity)
            String val = "'" + node.getValue().replace("\n", "\\n");
            if (val.length() > 80) {
                val = val.substring(0, 80) + "...";
            }
            // value identified with quotes to avoid umbiquity whe value looks like property
            out.append(val + "'");
        }
        out.append("\n");

        for (YamlStruct child : node.getChildren()) {
            renderNode(child, out);
        }
    }
}
