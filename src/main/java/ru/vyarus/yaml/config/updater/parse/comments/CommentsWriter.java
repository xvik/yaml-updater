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
        try {
            write(tree, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Can't write yaml to file", e);
        }
    }

    public static void write(final YamlTree tree, final OutputStream out) {
        final PrintWriter res = new PrintWriter(out);
        tree.getChildren().forEach(node -> writeNode(node, res));
        res.flush();
        res.close();
    }

    private static void writeNode(final YamlNode node, final PrintWriter out) {
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
            }
            if (node.getKey() != null) {
                if (node.isListItem()) {
                    // recover whitespace after dash
                    res += TreeStringUtils.whitespace(node.getKeyPadding() - node.getPadding() - 1);
                }

                res += node.getKey() + ':';
            }
            if (!node.getValue().isEmpty()) {
                res += node.getValue().get(0);
            }
            writeLine(node.getPadding(), res, out);

            // multiline value
            if (node.getValue().size() > 1) {
                for (int i = 1; i < node.getValue().size(); i++) {
                    // value line stored as-is
                    writeLine(0, node.getValue().get(i), out);
                }
            }

            // sub nodes
            for (YamlNode child : node.getChildren()) {
                writeNode(child, out);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write node: " + node, ex);
        }
    }

    private static void writeLine(final int padding, final String line, final PrintWriter out) {
        if (padding > 0) {
            out.write(TreeStringUtils.whitespace(padding));
        }
        out.write(line);
        out.write(System.lineSeparator());
    }
}
