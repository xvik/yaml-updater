package ru.vyarus.yaml.config.updater.comments;

import ru.vyarus.yaml.config.updater.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.comments.util.CountingIterator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class CommentsReader {

    public static YamlTree read(final File yaml) {
        try {
            return readLines(Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read file: " + yaml.getAbsolutePath(), e);
        }
    }

    public static YamlTree read(final String yaml) {
        try {
            return readLines(Arrays.asList(yaml.split("\\r?\\n")));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read yaml string", e);
        }
    }

    private static YamlTree readLines(List<String> lines) {
        final Context context = new Context();
        readNodes(new CountingIterator<>(lines.iterator()), context);
        return new YamlTree(context.rootNodes);
    }

    private static void readNodes(final CountingIterator<String> lines, final Context context) {
        while (lines.hasNext()) {
            final String line = lines.next();
            try {
                processLine(line, context);
            } catch (Exception ex) {
                throw new IllegalStateException("Error parsing line " + lines.getPosition(), ex);
            }
        }
        context.finish();
    }

    private static void processLine(final String line, final Context context) {
        final CharacterIterator chars = new StringCharacterIterator(line);
        try {
            while (chars.current() == ' ' && chars.next() != CharacterIterator.DONE) ;
            if (chars.getIndex() == chars.getEndIndex()) {
                // whitespace only: consider this as comment for simplicity to preserve overall structure
                context.comment(line);
            } else {
                int whitespace = chars.getIndex();
                switch (chars.current()) {
                    case '#':
                        // commented line (stored as is)
                        context.comment(line);
                        // todo detect commented property?
                        break;
                    case '-':
                        // list value: lists parsed by line, so in case of objects under list tick (dash),
                        // the property with tick would be a list value and later properties would be its children
                        // (theoretically incorrect structure, but completely normal (simple!) for current task)
                        context.listValue(whitespace, line.substring(chars.getIndex() + 1));
                        break;
                    default:
                        // property
                        parseProperty(context, whitespace, chars, line);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error parsing line on position " + (chars.getIndex() + 1) + ": "
                    + visualizeError(line, chars), ex);
        }
    }

    private static void parseProperty(final Context context,
                                      final int padding,
                                      final CharacterIterator chars,
                                      final String line) {
        // todo multiline value support (next lines, use boolean in context)
        // extracting property name
        while (chars.current() != ':' && chars.next() != CharacterIterator.DONE) ;
        if (chars.current() != ':') {
            throw new IllegalStateException("Property terminator ':' not found");
        }
        String name = line.substring(padding, chars.getIndex());
        String value = chars.getIndex() == chars.getEndIndex() ? null : line.substring(chars.getIndex() + 1);
        // todo multiline value detection (by ending)
        context.property(padding, name, value);
    }

    private static String visualizeError(final String line, final CharacterIterator chars) {
        String demo = "\n\t" + line + "\n\t";
        final int index = chars.getIndex();
        if (index > 1) {
            char[] array = new char[index - 1];
            Arrays.fill(array, '-');
            demo += new String(array);
        }
        demo += '^';
        return demo;
    }

    private static class Context {
        // storing only root nodes, sub nodes only required in context
        List<YamlNode> rootNodes = new ArrayList<>();
        YamlNode current;
        // comments aggregator
        List<String> comments = new ArrayList<>();
        List<String> value;

        public void comment(final String line) {
            comments.add(line);
        }

        public void listValue(final int padding, final String value) {
            // if current on same level then it was previous value and need to reference root
            YamlNode node = new YamlNode(current.getPadding() == padding ? current.getRoot() : current, padding);
            node.setListValue(true);
            // doesn't care here what is this (could be value or object)
            node.setValue(Collections.singletonList(value));
            flushComments(node);
            // node becomes current because list value could be an object
            current = node;
        }

        public void property(final int padding, final String name, final String value) {
            YamlNode root = null;
            // not true only for getting back from subtree to root level
            if (padding > 0 && current != null) {
                root = current;
                while (root != null && root.getPadding() >= padding) {
                    root = root.getRoot();
                }
            }
            YamlNode node = new YamlNode(root, padding);
            node.setName(name);
            // null in case of trailing comment node (name would also be null)
            if (value != null) {
                node.setValue(Collections.singletonList(value));
            }
            flushComments(node);
            current = node;
            if (root == null) {
                rootNodes.add(node);
            }
        }

        public void finish() {
            // save trailing comments as separate node
            if (!comments.isEmpty()) {
                property(0, null, null);
            }
        }

        private void flushComments(final YamlNode node) {
            if (!comments.isEmpty()) {
                node.getTopComment().addAll(comments);
                comments.clear();
            }
        }
    }
}
