package ru.vyarus.yaml.updater.parse.comments;

import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.comments.util.CountingIterator;
import ru.vyarus.yaml.updater.parse.comments.util.MultilineValue;
import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;

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
 * Custom yaml parser, preserving comments. Everything above property assumed to be it's comment.
 * Possible free comment at the end of file is parsed as special comment-only node.
 * <p>
 * Parser works by line. For values it does not parse exact value, instead remembers entire line to aggregate
 * possible in-line comments. Multi-line values are explicitly supported (but, also, all lines in multi line value
 * remembered completely to exactly reproduce original structure).
 * <p>
 * Parser assumed to be used on valid yaml file only (and so snakeyaml must be used first to validate file).
 *
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public final class CommentsReader {

    // assume explicit sequence and object notion as non parsable (keep and store as-is)
    public static final List<Character> VALUE_BOUNDARY_START = Arrays.asList('\'', '"', '{', '[');

    private CommentsReader() {
    }

    /**
     * @param yaml yaml file
     * @return parsed yaml model tree
     */
    public static CmtTree read(final File yaml) {
        try {
            return readLines(Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read file: " + yaml.getAbsolutePath(), e);
        }
    }

    /**
     * @param yaml yaml string
     * @return parsed yaml model tree
     */
    public static CmtTree read(final String yaml) {
        try {
            return readLines(Arrays.asList(yaml.split("\\r?\\n")));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read yaml string", e);
        }
    }

    private static CmtTree readLines(final List<String> lines) {
        final Context context = new Context();
        readNodes(new CountingIterator<>(lines.iterator()), context);
        return new CmtTree(context.rootNodes, lines.size());
    }

    private static void readNodes(final CountingIterator<String> lines, final Context context) {
        while (lines.hasNext()) {
            final String line = lines.next();
            try {
                context.lineNum = lines.getPosition();
                processLine(line, context);
            } catch (Exception ex) {
                throw new IllegalStateException("Error parsing line " + lines.getPosition(), ex);
            }
        }
        context.finish();
    }

    @SuppressWarnings({"checkstyle:NeedBraces", "checkstyle:EmptyStatement", "checkstyle:MultipleStringLiterals",
            "PMD.EmptyWhileStmt", "PMD.ControlStatementBraces"})
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
                parseValue(line, context, chars, whitespace);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error parsing line on position " + (chars.getIndex() + 1) + ": "
                    + visualizeError(line, chars), ex);
        }
    }

    @SuppressWarnings({"checkstyle:NeedBraces", "checkstyle:EmptyStatement",
            "PMD.EmptyWhileStmt", "PMD.ControlStatementBraces"})
    private static void parseValue(final String line,
                                   final Context context,
                                   final CharacterIterator chars,
                                   final int padding) {
        switch (chars.current()) {
            case '#':
                // commented line (stored as is)
                context.comment(line);
                break;
            case '-':
                // list value
                // if list contains sub-object, starting on the same line as tick then virtual node
                // created to store sub-object (in this case two objects would represent same line)
                // In case of empty dash, object encapsulated automatically

                // skip whitespace after dash
                while (chars.next() == ' ') ;

                // property-like structure might be quoted (simple string)
                Prop lprop = null;
                if (!VALUE_BOUNDARY_START.contains(chars.current())) {
                    lprop = parseProperty(chars, line);
                }
                if (lprop == null) {
                    // not a property (simple value); take everything after dash
                    lprop = new Prop(padding, null, line.substring(padding + 1));
                }
                // first property in list item or list constant
                context.listValue(padding, lprop);

                break;
            default:
                // flow multiline is when string value continues on new line without special markers
                if (!context.detectFlowMultiline(padding, line)) {
                    // property
                    final Prop prop = parseProperty(chars, line);
                    if (prop == null) {
                        throw new IllegalStateException("Property line expected, but no property found");
                    }
                    context.property(padding, prop);
                }
        }
    }

    private static Prop parseProperty(final CharacterIterator chars, final String line) {
        // stream assumed to be set at the beginning of possible property (after list dash or after whitespace)
        final int padding = chars.getIndex();
        final int comment = line.indexOf('#', padding);
        final int split = line.indexOf(':', padding);
        if (split < 0 || (comment > 0 && split > comment)) {
            // no property marker or it is in comment part - not a property
            return null;
        }
        final String name = line.substring(padding, split);
        // value may include in-line comment! pure value is not important
        final String value = split == chars.getEndIndex() ? null : line.substring(split + 1);
        final Prop res = new Prop(padding, name, value);
        // detecting multiline markers
        res.multiline = MultilineValue.detect(value);
        return res;
    }

    @SuppressWarnings({"checkstyle:MultipleStringLiterals", "PMD.UseStringBufferForStringAppends"})
    private static String visualizeError(final String line, final CharacterIterator chars) {
        String demo = "\n\t" + line + "\n\t";
        final int index = chars.getIndex();
        if (index > 1) {
            final char[] array = new char[index - 1];
            Arrays.fill(array, '-');
            demo += new String(array);
        }
        demo += '^';
        return demo;
    }

    @SuppressWarnings({"checkstyle:VisibilityModifier", "PMD.DefaultPackage"})
    private static class Prop {
        final int padding;
        final String key;
        final String value;
        MultilineValue.Marker multiline;

        Prop(final int padding, final String key, final String value) {
            this.padding = padding;
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }

    @SuppressWarnings({"checkstyle:VisibilityModifier", "PMD.DefaultPackage"})
    private static class Context {
        int lineNum;
        // storing only root nodes, sub nodes only required in context
        List<CmtNode> rootNodes = new ArrayList<>();
        CmtNode current;
        // comments aggregator
        List<String> comments = new ArrayList<>();
        MultilineValue.Marker multiline;

        public void comment(final String line) {
            comments.add(line);
        }

        public void listValue(final int padding, final Prop prop) {
            if (prop.key == null) {
                // scalar list value or sub object starts on new line (empty dash)
                property(padding, prop);
                YamlModelUtils.listItem(current);
            } else {
                // sub object starts from dash: using virtual list node to group object
                property(padding, null);
                YamlModelUtils.virtualListItem(current);
                // property becomes first child of virtual dash node
                property(prop.padding, prop);
            }
        }

        public void property(final int padding, final Prop prop) {
            final CmtNode root = YamlModelUtils.findNextLineRoot(padding, current);
            final CmtNode node = new CmtNode(root, padding, lineNum);
            // null in case of trailing comment node
            if (prop != null) {
                if (prop.key != null) {
                    node.setKey(prop.key);
                }
                if (prop.value != null) {
                    node.getValue().add(prop.value);
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
            } else if (multiline != null) {
                // obviously multiline ended

                // here we look for the last lines - it could be empty lines appended by mistake
                // (ending = 1 is a '+' case - greedy newline "eating" all whitespace after it)
                if (multiline.ending != 1) {
                    // in all other cases, whitespace lines must be re-considered as comments
                    final List<String> lines = current.getValue();
                    for(int i = lines.size() - 1; i > 0; i--) {
                        final String ln = lines.get(i);
                        // detaching empty ending line (but only with lesser indent)
                        if (ln.trim().isEmpty() && ln.length() < multiline.indent) {
                            // insert at 0 because we go backward
                            comments.add(0, lines.remove(i));
                            continue;
                        }
                        break;
                    }
                }

                multiline = null;
            }
            return false;
        }

        @SuppressWarnings("PMD.CollapsibleIfStatements")
        public boolean detectFlowMultiline(final int padding, final String line) {
            if (current != null) {
                // will go there only once for multiline value as after this multiline would be already detected,
                // aggregating everything below (by padding)
                if (current.getPadding() < padding && current.getValue() != null
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

        private void flushComments(final CmtNode node) {
            if (!comments.isEmpty()) {
                node.getTopComment().addAll(comments);
                comments.clear();
            }
        }
    }
}
