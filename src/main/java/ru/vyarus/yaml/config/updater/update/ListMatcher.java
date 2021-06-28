package ru.vyarus.yaml.config.updater.update;

import ru.vyarus.yaml.config.updater.parse.common.model.YamlLine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * List matching utilities. Assuming list items might be reordered in yaml files. Also, updating file could contain
 * more (or less) properties.
 * <p>
 * It is important to update list items because in complex configs, list items contain entire subtrees which might
 * also change (new properties added, comments changed, etc).
 * <p>
 * Searches for items with the maximum number of similar values. Use values from snakeyaml parser to increase
 * accuracy. If items contains subtrees - apply same matching logic for entire subtree.
 * <p>
 * Note that matching should work in both directions: find old node in new file's list or an opposite (no matter how
 * items were changed).
 *
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
public final class ListMatcher {

    private ListMatcher() {
    }

    /**
     * Replaces list item positions with asterisk, so list items from different files (different list positions)
     * could be compared.
     *
     * @param path yaml path
     * @return yaml path with unified list positions
     */
    public static String unifyListItemPath(String path) {
        return path.replaceAll("\\[\\d+]", "[*]");
    }

    /**
     * Searches for matched list item in the items list (assuming item from one file and list from another, no matter
     * what direction).
     * <p>
     * Matches complete subtrees. Searches for items with the maximum amount of the same properties containing same
     * values. If value would differ in any property - item would not be matched,
     * <p>
     * NOTE: if item matches with multiple nodes in the list, null will be returned because match must be exact!
     * Otherwise there is a high chance to incorrectly merge file (better not merge part at all).
     * <p>
     * IMPORTANT: to avoid matching same items for different targets, remove matched item from candidates list.
     *
     * @param node list item node to find match for
     * @param list collection of list items to find matching in
     * @param <T>  structure type (works for both comments and snakeyaml structures)
     * @return matched item or null
     */
    public static <T extends YamlLine<T>> T match(T node, List<T> list) {
        final List<T> cand = new ArrayList<>(list);
        // count items matched at least by one property (to filter completely different items)
        // line num is unique identity for line item
        final Set<Integer> matchedItems = new HashSet<>();

        // using as much properties as required to find unique match
        for (T prop : node.getChildren()) {
            // not subtree and no value - can't be used for matching
            if (!prop.hasChildren() && prop.getIdentityValue() == null) {
                continue;
            }
            final Iterator<T> it = cand.iterator();
            // searching matched item by one prop (from previously selected nodes)
            while (it.hasNext()) {
                final T cnd = it.next();
                boolean match = false;
                boolean propFound = false;
                for (T uprop : cnd.getChildren()) {
                    if (prop.getKey().equals(uprop.getKey())) {
                        propFound = true;
                        try {
                            if (matches(prop, uprop)) {
                                match = true;
                                matchedItems.add(cnd.getLineNum());
                            }
                        } catch (Exception ex) {
                            throw new IllegalStateException("Failed to compare '" + prop.getYamlPath()
                                    + "' list item property", ex);
                        }
                        break;
                    }
                }
                // avoid removing items where tested property was missing (maybe other props would match)
                if (propFound && !match) {
                    it.remove();
                }
            }
            if (cand.isEmpty()) {
                // nothing matched or exactly one match
                break;
            }
        }

        // filter candidates without any match (to avoid false matching for totally different lists)
        cand.removeIf(candNode -> !matchedItems.contains(candNode.getLineNum()));

        // search for EXACT match
        return cand.size() == 1 ? cand.get(0) : null;
    }

    private static <T extends YamlLine<T>> boolean matches(final T a, final T b) {
        if (a.hasListValue() && !a.getChildren().get(0).isObjectListItem()) {
            // special case: scalar lists are not merged and so should not be compared
            // but have assume this case as match
            return true;
        }
        // subtree matching
        if (a.hasChildren()) {
            if (!b.hasChildren()) {
                return false;
            }
            int matches = 0;
            // all props found in left subtree must match props in the right subtree
            // (left prop may not be found on the right, but at least one property must match)
            for (T aprop : a.getChildren()) {
                if (!aprop.isProperty()) {
                    continue;
                }
                boolean propFound = false;
                boolean match = false;
                for (T bprop : b.getChildren()) {
                    if (aprop.getKey().equals(bprop.getKey())) {
                        propFound = true;
                        // could be deeper subtree check
                        match = matches(aprop, bprop);
                        if (!match) {
                            break;
                        } else {
                            matches++;
                        }
                    }
                }
                // found at least one not matched property (different value)
                if (propFound && !match) {
                    return false;
                }
            }
            // at least one property
            return matches > 0;
        }
        // direct value matching
        return a.getIdentityValue().equals(b.getIdentityValue());
    }
}
