package ru.vyarus.yaml.updater.parse.struct;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;
import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;
import ru.vyarus.yaml.updater.parse.struct.model.StructNode;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Snakeyaml-based parser. Builds exactly the same structure as comments parser (to simplify comparisons).
 * <p>
 * Object list items are split into "dash" object and properties as children. This way item object structure could
 * be completely preserved.
 *
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public final class StructureReader {

    private StructureReader() {
    }

    /**
     * @param file yaml file
     * @return parsed yaml model tree
     */
    public static StructTree read(final File file) {
        // comments parser does not support multiple yaml documents because this is not common for configs
        // so parsing only the first document, ignoring anything else
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return read(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse yaml file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * @param file yaml file as string
     * @return parsed yaml model tree
     */
    public static StructTree read(final String file) {
        return read(new StringReader(file));
    }

    /**
     * @param reader yaml content reader
     * @return parsed yaml model tree
     */
    public static StructTree read(final Reader reader) {
        try {
            final Node node = new Yaml().compose(reader);
            // null when file is empty
            if (node != null) {
                final Context context = new Context();
                processNode(node, context);
                return new StructTree(context.rootNodes, node.getEndMark().getLine() + 1);
            } else {
                return new StructTree(new ArrayList<>(), 0);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse yaml structure", e);
        }
    }

    private static void processNode(final Node node, final Context context) {
        if (node instanceof CollectionNode
                && DumperOptions.FlowStyle.FLOW.equals(((CollectionNode<?>) node).getFlowStyle())) {
            // special case when object declared in FLOW style: {one: 1, two: 2} or [1, 2, 3]
            // in this case assuming entire object as single value - mapping it to string
            context.current.setValue(parseFlowObject((CollectionNode<?>) node));
            // lists items might be considered as objects, which is not the case
            context.current.setListItemWithProperty(false);
        } else if (node instanceof MappingNode) {
            final MappingNode container = (MappingNode) node;

            for (NodeTuple tuple : container.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode)) {
                    throw new IllegalStateException("Unsupported key node type " + tuple.getKeyNode()
                            + " in tuple " + tuple);
                }
                final ScalarNode key = (ScalarNode) tuple.getKeyNode();
                context.lineNum = key.getStartMark().getLine() + 1;
                String value = null;
                if (tuple.getValueNode() instanceof ScalarNode) {
                    value = ((ScalarNode) tuple.getValueNode()).getValue();
                }
                context.property(key.getStartMark().getColumn(), key.getValue(), value);

                // lists or sub objects
                if (!(tuple.getValueNode() instanceof ScalarNode)) {
                    processNode(tuple.getValueNode(), context);
                }
            }
        } else if (node instanceof SequenceNode) {
            processSequence((SequenceNode) node, context);
        } else {
            throw new IllegalStateException("Unsupported node type: " + node);
        }
    }

    private static String parseFlowObject(final CollectionNode<?> node) {
        final StringWriter out = new StringWriter();

        // the code below is taken from Yaml#serialize(node, out) appeared only in recent snakeyaml versions
        // (used like this for better compatibility with older snakeyaml versions (older dropwizard))
        final DumperOptions options = new DumperOptions();
        final Serializer serializer = new Serializer(new Emitter(out, options), new Resolver(), options, null);
        try {
            serializer.open();
            serializer.serialize(node);
            serializer.close();
        } catch (IOException e) {
            throw new YAMLException(e);
        }

        // trim cut's off trailing newline
        return out.toString().trim();
    }

    private static void processSequence(final SequenceNode node, final Context context) {
        // list value
        for (Node seq : node.getValue()) {
            // need position of dash, which is absent here, so just assuming -2 shift from value
            final int listPad = seq.getStartMark().getColumn() - 2;

            context.lineNum = seq.getStartMark().getLine() + 1;
            if (seq instanceof ScalarNode) {
                // simple value
                context.listValue(listPad, ((ScalarNode) seq).getValue());
            } else {
                final boolean tickSameLine = seq.getStartMark().get_snippet().trim().charAt(0) == '-';
                if (!tickSameLine) {
                    // case when properties start after empty dash (next line)
                    // and hierarchically it must be reproduced (unification with comments parser)

                    // it is impossible to know EXACTLY what line is tick on, but in most cases it would be previous
                    context.lineNum--;
                    context.listValue(listPad, null);
                } else {
                    // sub object: use virtual node (indicating dash) to group sub-object properties
                    context.virtualListItemNode(listPad);
                }

                processNode(seq, context);
            }
        }
    }

    @SuppressWarnings({"checkstyle:VisibilityModifier", "PMD.DefaultPackage"})
    private static final class Context {
        int lineNum;
        List<StructNode> rootNodes = new ArrayList<>();
        StructNode current;

        public void property(final int padding, final String name, final String value) {
            final StructNode root = YamlModelUtils.findNextLineRoot(padding, current);
            final StructNode node = new StructNode(root, padding, lineNum);
            if (name != null) {
                node.setKey(name);
            }
            if (value != null) {
                node.setValue(value);
            }
            current = node;
            if (root == null) {
                rootNodes.add(node);
            }
        }

        public void listValue(final int padding, final String value) {
            property(padding, null, value);
            YamlModelUtils.listItem(current);
        }

        public void virtualListItemNode(final int padding) {
            property(padding, null, null);
            YamlModelUtils.virtualListItem(current);
        }
    }
}
