package ru.vyarus.yaml.config.updater.struct;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;
import ru.vyarus.yaml.config.updater.struct.model.YamlStruct;
import ru.vyarus.yaml.config.updater.struct.model.YamlStructTree;

import java.io.File;
import java.io.FileReader;
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
        try {
            final Node node = new Yaml().compose(new FileReader(file));
            final Context context = new Context();
            processNode(node, context);
            return new YamlStructTree(context.rootNodes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse yaml structure: " + file.getAbsolutePath(), e);
        }
    }

    private static void processNode(final Node node, final Context context) {
        if (node instanceof MappingNode) {
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode)) {
                    throw new IllegalStateException("Unsupported key node type " + tuple.getKeyNode()
                            + " in tuple " + tuple);
                }
                ScalarNode key = (ScalarNode) tuple.getKeyNode();
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
                if (node instanceof ScalarNode) {
                    // simple value
                    // todo think how to identify lists
                } else {
                    // sub object
                    // todo INCORRECT: think how to identify objects as list values
                    processNode(seq, context);
                }
            }
        } else {
            throw new IllegalStateException("Unsupported node type: " + node);
        }
    }

    private static class Context {
        List<YamlStruct> rootNodes = new ArrayList<>();
        YamlStruct current;

        public void property(final int padding, final String name, final String value) {
            YamlStruct root = null;
            // not true only for getting back from subtree to root level
            if (padding > 0 && current != null) {
                root = current;
                while (root != null && root.getPadding() >= padding) {
                    root = root.getRoot();
                }
            }
            YamlStruct node = new YamlStruct(root, padding);
            node.setName(name);
            if (value != null) {
                node.setValue(value);
            }
            current = node;
            if (root == null) {
                rootNodes.add(node);
            }
        }
    }
}
