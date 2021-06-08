package ru.vyarus.yaml.config.updater.parse.comments;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.comments.util.TreeStringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Vyacheslav Rusakov
 * @since 28.04.2021
 */
public class CommentsWriter {

    public static String write(final YamlTree tree) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(tree, out);
        try {
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to convert stream", e);
        }
    }

    public static void write(final YamlTree tree, File file) {
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8.name())) {
            write(tree, out);
        } catch (Exception e) {
            throw new IllegalStateException("Can't write yaml to file", e);
        }
    }

    public static void write(final YamlTree tree, final OutputStream out) {
        write(tree, new PrintWriter(out));
    }

    private static void write(final YamlTree tree, final PrintWriter writer) {
        tree.getChildren().forEach(node -> writeNode(node, writer, false));
        writer.flush();
        writer.close();
    }

    private static void writeNode(final YamlNode node, final PrintWriter out, final boolean listItemFirstLine) {
        try {
            // starting with comment
            for (String comment : node.getTopComment()) {
                // comment line stored as-is (all paddings preserved)
                writeLine(0, comment, out);
            }
            if (node.isCommentOnly()) {
                return;
            }

            String res = "";
            if (node.isListItem()) {
                res += "-";
                if (node.isListItemWithProperty()) {
                    // recover whitespace after dash
                    res += TreeStringUtils.whitespace(node.getChildren().get(0).getPadding() - node.getPadding() - 1);
                }
            }
            if (node.getKey() != null) {
                res += node.getKey() + ':';
            }
            if (!node.getValue().isEmpty()) {
                res += node.getValue().get(0);
            }
            if (node.isListItemWithProperty()) {
                // without new line (property must be written on the same line)
                write(node.getPadding(), res, out);
            } else {
                // case when property is a first list item property written just after dash
                // in this case padding already written on line (during dash node rendering)
                writeLine(listItemFirstLine ? 0: node.getPadding(), res, out);
            }

            // multiline value
            if (node.getValue().size() > 1) {
                for (int i = 1; i < node.getValue().size(); i++) {
                    // value line stored as-is
                    writeLine(0, node.getValue().get(i), out);
                }
            }

            // sub nodes
            // for split list node need to merge it properly into single line
            boolean avoidPadding = node.isListItemWithProperty();
            for (YamlNode child : node.getChildren()) {
                writeNode(child, out, avoidPadding);
                avoidPadding = false;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write node: " + node, ex);
        }
    }

    private static void write(final int padding, final String line, final PrintWriter out) {
        if (padding > 0) {
            out.write(TreeStringUtils.whitespace(padding));
        }
        out.write(line);
    }

    private static void writeLine(final int padding, final String line, final PrintWriter out) {
        write(padding, line, out);
        out.write(System.lineSeparator());
    }
}
