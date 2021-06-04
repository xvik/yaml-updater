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
            getChildren().forEach(node -> renderNode(node, out, false));
        }

        return out.toString();
    }

    private void renderNode(final YamlStruct node, final StringBuilder out, final boolean listItemFirstLine) {
        if (!listItemFirstLine) {
            out.append(TreeStringUtils.whitespace(node.getPadding()));
        }
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
        // for objects in list node virtual node created, splitting line into two objects (if no empty dash used)
        if (!node.isListItem() || !node.isListItemWithProperty()) {
            out.append("\n");
        }

        boolean avoidPadding = node.isListItemWithProperty();
        for (YamlStruct child : node.getChildren()) {
            // render dash and first item property as single line (when avoidPadding = true)
            renderNode(child, out, avoidPadding);
            avoidPadding = false;
        }
    }
}
