package ru.vyarus.yaml.config.updater.parse.comments;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.comments.util.CountingIterator;
import ru.vyarus.yaml.config.updater.parse.comments.util.MultilineValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
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
            final int whitespace = chars.getIndex();
            final boolean whitespaceOnly = chars.getIndex() == chars.getEndIndex();
            if (context.detectMultilineValue(whitespace, whitespaceOnly, line)) {
                // multiline value continues
                return;
            }
            if (whitespaceOnly) {
                // whitespace only: consider this as comment for simplicity to preserve overall structure
                // NOTE this might be the second line of flow multiline value, which is impossible to know before
                // looking next line
                context.comment(line);
            } else {
                switch (chars.current()) {
                    case '#':
                        // commented line (stored as is)
                        context.comment(line);
                        // todo detect commented property?
                        break;
                    case '-':
                        // list value: lists parsed by line, so in case of objects under list tick (dash),
                        // the property with tick would be a list value and later properties would be its children
                        // (theoretically incorrect structure, but completely normal for current task)

                        // skip whitespace after dash
                        while (chars.next() == ' ') ;

                        // property-like structure might be quoted (simple string)
                        Prop lprop = null;
                        if (chars.current() != '\'' || chars.current() != '"') {
                            lprop = parseProperty(chars, line);
                        }
                        if (lprop == null) {
                            // not a property (simple value); take everything after dash
                            lprop = new Prop(whitespace, null, line.substring(whitespace + 1));
                        }
                        // first property in list item or list constant
                        context.listValue(whitespace, lprop);

                        break;
                    default:
                        // flow multiline is when string value continues on new line without special markers
                        if (!context.detectFlowMultiline(whitespace, line)) {
                            // property
                            final Prop prop = parseProperty(chars, line);
                            if (prop == null) {
                                throw new IllegalStateException("Property line expected, but no property found");
                            }
                            context.property(whitespace, prop);
                        }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error parsing line on position " + (chars.getIndex() + 1) + ": "
                    + visualizeError(line, chars), ex);
        }
    }

    private static Prop parseProperty(final CharacterIterator chars, final String line) {
        // stream assumed to be set at the beginning of possible property (after list dash or after whitespace)
        int padding = chars.getIndex();
        int comment = line.indexOf('#', padding);
        int split = line.indexOf(':', padding);
        if (split < 0 || (comment > 0 && split > comment)) {
            // no property marker or it is in comment part - not a property
            return null;
        }
        String name = line.substring(padding, split);
        // value may include in-line comment! pure value is not important
        String value = split == chars.getEndIndex() ? null : line.substring(split + 1);
        final Prop res = new Prop(padding, name, value);
        // detecting multiline markers
        res.multiline = MultilineValue.detect(value);
        return res;
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

    private static class Prop {
        public final int padding;
        public final String key;
        public final String value;
        public MultilineValue.Marker multiline;

        public Prop(final int padding, final String key, final String value) {
            this.padding = padding;
            this.key = key;
            this.value = value;
        }
    }

    private static class Context {
        // storing only root nodes, sub nodes only required in context
        List<YamlNode> rootNodes = new ArrayList<>();
        YamlNode current;
        // comments aggregator
        List<String> comments = new ArrayList<>();
        MultilineValue.Marker multiline;

        public void comment(final String line) {
            comments.add(line);
        }

        public void listValue(final int padding, final Prop prop) {
            property(padding, prop);
            current.setListValue(true);
        }

        public void property(final int padding, final Prop prop) {
            // close ongoing multiline (value is already aggregated)
            if (multiline != null) {
                multiline = null;
            }

            YamlNode root = null;
            // not true only for getting back from subtree to root level
            if (padding > 0 && current != null) {
                root = current;
                while (root != null && root.getPadding() >= padding) {
                    root = root.getRoot();
                }
            }
            final YamlNode node = new YamlNode(root, padding);
            // null in case of trailing comment node
            if (prop != null) {
                if (prop.key != null) {
                    node.setKey(prop.key);
                    // this is only important for lists where dash padding used as root
                    // (required only for flow multiline values detection)
                    node.setKeyPadding(prop.padding);
                }
                if (prop.value != null) {
                    final List<String> valList = new ArrayList<>();
                    valList.add(prop.value);
                    node.setValue(valList);
                }

                // remember multiline marker it it was detected in value
                multiline = prop.multiline;
            }
            flushComments(node);
            current = node;
            if (root == null) {
                rootNodes.add(node);
            }
        }

        public boolean detectMultilineValue(final int padding, final boolean whitespaceOnly, final String line) {
            if (multiline != null && (whitespaceOnly || multiline.indent <= padding)) {
                current.getValue().add(line);
                if (multiline.indent == -1) {
                    // indent computed by the first line (multiline defined, but without number (| or >))
                    multiline.indent = padding;
                }
                return true;
            }
            return false;
        }

        public boolean detectFlowMultiline(final int padding, final String line) {
            if (current != null) {
                // will go there only once for multiline value as after this multiline would be already detected,
                // aggregating everything below (by padding)
                if (current.getKeyPadding() < padding && current.getValue() != null
                        && MultilineValue.couldBeFlowMultiline(current.getValue().get(0))) {
                    if (!comments.isEmpty()) {
                        // edge case: when the second (and maybe few following) lines of flow multiline value
                        // are empty lines, it is impossible to detect as multiline, and they would go to comment
                        // instead
                        current.getValue().addAll(comments);
                        comments.clear();
                    }
                    current.getValue().add(line);
                    multiline = MultilineValue.flowMarker(padding);
                    return true;
                }
            }
            return false;
        }

        public void finish() {
            // save trailing comments as separate node
            if (!comments.isEmpty()) {
                property(0, null);
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
