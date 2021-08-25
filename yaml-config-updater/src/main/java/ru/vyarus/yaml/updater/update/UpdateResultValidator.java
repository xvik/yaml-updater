package ru.vyarus.yaml.updater.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.updater.parse.struct.model.StructNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates merged file against old and new configs: all old values must be preserved and all new values must be
 * added. List items are also checked.
 * <p>
 * Use snakeyaml-based models for correct values comparison.
 *
 * @author Vyacheslav Rusakov
 * @since 17.05.2021
 */
public final class UpdateResultValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateResultValidator.class);

    private UpdateResultValidator() {
    }

    /**
     * Validate merge result correctness.
     *
     * @param merged merge result (just read)
     * @param old    old yaml
     * @param update new yaml
     */
    public static void validate(final TreeNode<StructNode> merged,
                                final TreeNode<StructNode> old,
                                final TreeNode<StructNode> update) {
        final Set<String> checked = new HashSet<>();
        // for list items it is important to cut off path before item and search by sub path only
        // (because items would be on different indexes and so paths would be different in trees)
        final String rootPath = merged.getYamlPath();
        for (StructNode leaf : merged.getTreeLeaves()) {
            final String fullYamlPath = leaf.getYamlPath();
            final String yamlPath = fullYamlPath.substring(rootPath != null ? rootPath.length() + 1 : 0);
            // ignore list positions in path (for list items correct items already selected)
            checked.add(ListMatcher.unifyListItemPath(fullYamlPath));

            // nulls could appear when matching list items
            final StructNode oldNode = old != null ? old.find(yamlPath) : null;
            final StructNode newNode = update != null ? update.find(yamlPath) : null;

            if (leaf.hasListValue()) {
                validateList(leaf, oldNode, newNode);
                continue;
            }
            assertValue(leaf, fullYamlPath, oldNode, newNode);
        }

        checkMissedValues(old, update, checked);
    }

    private static void validateList(final StructNode list,
                                     final StructNode oldList,
                                     final StructNode newList) {
        for (StructNode item : list.getChildren()) {
            if (!item.isObjectListItem()) {
                // scalar lists not merged
                continue;
            }
            LOGGER.debug("Searching list item {} in current file", item.getYamlPath());
            final StructNode oldItem = ListMatcher.match(item, oldList.getChildren());
            LOGGER.debug("Searching list item {} in update file", item.getYamlPath());
            final StructNode newItem = ListMatcher.match(item, newList.getChildren());
            if (oldItem == null && newItem == null) {
                throw new IllegalStateException("Can't find reference list item neither in old nor in new file: "
                        + item.getYamlPath());
            }
            validate(item, oldItem, newItem);
        }
    }

    private static void assertValue(final StructNode leaf,
                                    final String fullYamlPath,
                                    final StructNode oldNode,
                                    final StructNode newNode) {
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
                        "Property '%s' not found neither in old nor in new file: '%s'",
                        fullYamlPath, leaf.getValue()));
            }
            if (!leaf.getValue().equals(newNode.getValue())) {
                throw new IllegalStateException(String.format(
                        "Invalid value on path '%s': '%s' when should be from update file '%s'",
                        fullYamlPath, leaf.getValue(), newNode.getValue()));
            }
        }
    }

    private static void checkMissedValues(final TreeNode<StructNode> old,
                                          final TreeNode<StructNode> update,
                                          final Set<String> checked) {
        // check for missed values (which should not be removed)
        if (old != null) {
            for (StructNode node : old.getTreeLeaves()) {
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
            for (StructNode node : update.getTreeLeaves()) {
                final String yamlPath = node.getYamlPath();
                if (!checked.contains(ListMatcher.unifyListItemPath(yamlPath))) {
                    throw new IllegalStateException(String.format(
                            "Value '%s' from update file was not added: '%s'",
                            yamlPath, node.getValue()));
                }
            }
        }
    }
}
