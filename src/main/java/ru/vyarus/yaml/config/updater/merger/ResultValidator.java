package ru.vyarus.yaml.config.updater.merger;

import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStruct;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vyacheslav Rusakov
 * @since 17.05.2021
 */
public class ResultValidator {

    public static void validate(final YamlStructTree merged,
                                final YamlStructTree old,
                                final YamlStructTree update) {
        final Set<String> checked = new HashSet<>();
        for (YamlStruct leaf : merged.getTreeLeaves()) {
            final String yamlPath = leaf.getYamlPath();
            checked.add(yamlPath);
            if (leaf.hasListValue()) {
                // todo no lists verification
                continue;
            }
            YamlStruct oldNode = old.find(yamlPath);
            if (oldNode != null) {
                // verify with old file (survived value)
                if (!leaf.getValue().equals(oldNode.getValue())) {
                    throw new IllegalStateException(String.format(
                            "Invalid value on path '%s': '%s' when should remain from old file '%s'",
                            yamlPath, leaf.getValue(), oldNode.getValue()));
                }
            } else {
                // if not in old file, then it's a merged value from new file
                YamlStruct node = update.find(yamlPath);
                if (node == null) {
                    throw new IllegalStateException(String.format(
                            "Value '%s' not found neither in old nor in new file: '%s'",
                            yamlPath, leaf.getValue()));
                }
                if (!leaf.getValue().equals(node.getValue())) {
                    throw new IllegalStateException(String.format(
                            "Invalid value on path '%s': '%s' when should be from update file '%s'",
                            yamlPath, leaf.getValue(), node.getValue()));
                }
            }
        }

        // check for missed values (which should not be removed)
        for (YamlStruct node : old.getTreeLeaves()) {
            final String yamlPath = node.getYamlPath();
            if (!checked.contains(yamlPath)) {
                throw new IllegalStateException(String.format(
                        "Value '%s' disappeared (should remain from original file): '%s'",
                        yamlPath, node.getValue()));
            }
        }

        // check for mot added values from update file
        for (YamlStruct node : update.getTreeLeaves()) {
            final String yamlPath = node.getYamlPath();
            if (!checked.contains(yamlPath)) {
                throw new IllegalStateException(String.format(
                        "Value '%s' from update file was not added: '%s'",
                        yamlPath, node.getValue()));
            }
        }
    }
}
