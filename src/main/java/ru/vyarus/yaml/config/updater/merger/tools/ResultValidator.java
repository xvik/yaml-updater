package ru.vyarus.yaml.config.updater.merger.tools;

import ru.vyarus.yaml.config.updater.parse.model.TreeNode;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStruct;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vyacheslav Rusakov
 * @since 17.05.2021
 */
public class ResultValidator {

    public static void validate(final TreeNode<YamlStruct> merged,
                                final TreeNode<YamlStruct> old,
                                final TreeNode<YamlStruct> update) {
        final Set<String> checked = new HashSet<>();
        // for list items it is important to cut off path before item and search by sub path only
        // (because items would be on different indexes and so paths would be different in trees)
        final String rootPath = merged instanceof YamlStruct ? ((YamlStruct) merged).getYamlPath() : null;
        for (YamlStruct leaf : merged.getTreeLeaves()) {
            final String fullYamlPath = leaf.getYamlPath();
            final String yamlPath = fullYamlPath.substring(rootPath != null ? rootPath.length() + 1 : 0);
            // ignore list positions in path (for list items correct items already selected)
            checked.add(ListMatcher.unifyListItemPath(fullYamlPath));

            // nulls could be when matching list items
            YamlStruct oldNode = old != null ? old.find(yamlPath) : null;
            YamlStruct newNode = update != null ? update.find(yamlPath) : null;

            if (leaf.hasListValue()) {
                validateList(leaf, oldNode, newNode);
                continue;
            }
            if (oldNode != null) {
                // verify with old file (survived value)
                if (!leaf.getValue().equals(oldNode.getValue())) {
                    throw new IllegalStateException(String.format(
                            "Invalid value on path '%s': '%s' when should remain from old file '%s'",
                            fullYamlPath, leaf.getValue(), oldNode.getValue()));
                }
            } else {
                // if not in old file, then it's a merged value from new file
                if (newNode == null) {
                    throw new IllegalStateException(String.format(
                            "Value '%s' not found neither in old nor in new file: '%s'",
                            fullYamlPath, leaf.getValue()));
                }
                if (!leaf.getValue().equals(newNode.getValue())) {
                    throw new IllegalStateException(String.format(
                            "Invalid value on path '%s': '%s' when should be from update file '%s'",
                            fullYamlPath, leaf.getValue(), newNode.getValue()));
                }
            }
        }

        // check for missed values (which should not be removed)
        if (old != null) {
            for (YamlStruct node : old.getTreeLeaves()) {
                final String yamlPath = node.getYamlPath();
                if (!checked.contains(ListMatcher.unifyListItemPath(yamlPath))) {
                    throw new IllegalStateException(String.format(
                            "Value '%s' disappeared (should remain from original file): '%s'",
                            yamlPath, node.getValue()));
                }
            }
        }

        // check for not added values from update file
        if (update != null) {
            for (YamlStruct node : update.getTreeLeaves()) {
                final String yamlPath = node.getYamlPath();
                if (!checked.contains(ListMatcher.unifyListItemPath(yamlPath))) {
                    throw new IllegalStateException(String.format(
                            "Value '%s' from update file was not added: '%s'",
                            yamlPath, node.getValue()));
                }
            }
        }
    }

    private static void validateList(final YamlStruct list,
                                     final YamlStruct oldList,
                                     final YamlStruct newList) {
        for (YamlStruct item : list.getChildren()) {
            if (!item.isObjectListItem()) {
                // scalar lists not merged
                continue;
            }
            final YamlStruct oldItem = ListMatcher.match(item, oldList.getChildren());
            final YamlStruct newItem = ListMatcher.match(item, newList.getChildren());
            if (oldItem == null && newItem == null) {
                throw new IllegalStateException("Can't find reference list item neither in old nor in new file: "
                        + item.getYamlPath());
            }
            validate(item, oldItem, newItem);
        }
    }
}
