package ru.vyarus.yaml.config.updater.parse.struct;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import ru.vyarus.yaml.config.updater.parse.common.YamlModelUtils;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStruct;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
public class StructureReader {

    public static YamlStructTree read(final File file) {
        // comments parser does not support multiple yaml documents because this is not common for configs
        // so parsing only the first document, ignoring anything else
        try (FileInputStream in = new FileInputStream(file)) {
            return read(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse yaml file: " + file.getAbsolutePath(), e);
        }
    }

    public static YamlStructTree read(final Reader reader) {
        try {
            final Node node = new Yaml().compose(reader);
            final Context context = new Context();
            processNode(node, context);
            return new YamlStructTree(context.rootNodes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse yaml structure", e);
        }
    }

    private static void processNode(final Node node, final Context context) {
        if (node instanceof MappingNode) {
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
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
            // list value
            for (Node seq : ((SequenceNode) node).getValue()) {
                // need position of dash, which is absent here, so just assuming -2 shift from value
                final int listPad = seq.getStartMark().getColumn() - 2;

                context.lineNum = seq.getStartMark().getLine() + 1;
                if (seq instanceof ScalarNode) {
                    // simple value
                    context.listValue(listPad, ((ScalarNode) seq).getValue());
                } else {
                    boolean tickSameLine = seq.getStartMark().get_snippet().trim().startsWith("-");
                    if (!tickSameLine) {
                        // case when properties start after empty dash (next line)
                        // and hierarchically it must be reproduced (unification with comments parser)
                        context.listValue(listPad, null);
                    } else {
                        // sub object: use virtual node (indicating dash) to group sub-object properties
                        context.virtualListItemNode(listPad);
                    }

                    processNode(seq, context);
                }
            }
        } else {
            throw new IllegalStateException("Unsupported node type: " + node);
        }
    }

    private static class Context {
        int lineNum;
        List<YamlStruct> rootNodes = new ArrayList<>();
        YamlStruct current;

        public void property(final int padding, final String name, final String value) {
            final YamlStruct root = YamlModelUtils.findNextLineRoot(padding, current);
            final YamlStruct node = new YamlStruct(root, padding, lineNum);
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
