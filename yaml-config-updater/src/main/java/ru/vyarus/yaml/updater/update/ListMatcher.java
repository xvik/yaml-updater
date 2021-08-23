package ru.vyarus.yaml.updater.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.updater.parse.common.model.YamlLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * List matching utilities. Assuming list items might be reordered in yaml files. Also, updating file could contain
 * more (or less) properties.
 * <p>
 * It is important to update list items because in complex configs, list items contain entire subtrees which might
 * also change (new properties added, comments changed, etc.).
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ListMatcher.class);

    private ListMatcher() {
    }

    /**
     * Replaces list item positions with asterisk, so list items from different files (different list positions)
     * could be compared.
     *
     * @param path yaml path
     * @return yaml path with unified list positions
     */
    public static String unifyListItemPath(final String path) {
        return path.replaceAll("\\[\\d+]", "[*]");
    }

    /**
     * Searches for matched list item in the items list (assuming item from one file and list from another, no matter
     * what direction).
     * <p>
     * Matches complete subtrees. Searches for items with the maximum amount of the same properties containing same
     * values. If value would differ in any property - item would not be matched,
     * <p>
     * Scalar list values are ignored (not comparing such properties). For lists with objects, at least one item must
     * match in target list (with the same semantics as above).
     * <p>
     * NOTE: if item matches with multiple nodes in the list, null will be returned because match must be exact!
     * Otherwise, there is a high chance to incorrectly merge file (better not merge part at all).
     * <p>
     * IMPORTANT: to avoid matching same items for different targets, remove matched item from candidates list.
     *
     * @param node list item node to find match for
     * @param list collection of list items to find matching in
     * @param <T>  structure type (works for both comments and snakeyaml structures)
     * @return matched item or null
     */
    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity",
            "PMD.CyclomaticComplexity"})
    public static <T extends YamlLine<T>> T match(final T node, final List<T> list) {
        LOGGER.trace("Searching for matching list item {}", node.getYamlPath());
        final List<T> cand = new ArrayList<>(list);
        // count items match count (how many props match) to filter completely different items
        // line num is unique identity for line item
        final Map<Integer, Integer> matchedItems = new HashMap<>();

        // using as many properties as required to find unique match
        for (T prop : node.getChildren()) {
            // not subtree and no value - can't be used for matching
            if (!prop.hasChildren() && prop.getIdentityValue() == null) {
                LOGGER.trace("\tempty property {} can't be used for matching", prop.getYamlPath());
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
                                final int lineNum = cnd.getLineNum();
                                final int matches = (matchedItems.getOrDefault(lineNum, 0)) + 1;
                                matchedItems.put(lineNum, matches);
                                LOGGER.trace("\tmatch found: {}", prop);
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
                    LOGGER.trace("\tcandidate denied: {}", cnd.getYamlPath());
                    it.remove();
                }
            }
            if (cand.isEmpty()) {
                // nothing matched or exactly one match
                break;
            }
        }

        filterCandidates(node, cand, matchedItems);

        // search for EXACT match
        return cand.size() == 1 ? cand.get(0) : null;
    }

    private static <T extends YamlLine<T>> void filterCandidates(final T node,
                                                                 final List<T> cand,
                                                                 final Map<Integer, Integer> matchedItems) {
        // filter candidates without any match (to avoid false matching for totally different lists)
        cand.removeIf(candNode -> !matchedItems.containsKey(candNode.getLineNum()));

        if (cand.size() > 1) {
            // selecting nodes with maximum matches
            final int max = Collections.max(matchedItems.values());
            cand.removeIf(candNode -> matchedItems.get(candNode.getLineNum()) != max);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} matched items found for {}: {}", cand.size(), node.getYamlPath(),
                    cand.stream().map(YamlLine::getYamlPath).collect(Collectors.toList()));
        }
    }

    private static <T extends YamlLine<T>> boolean matches(final T a, final T b) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("\tmatching {} ({}) with {} ({})", a.getYamlPath(), a, b.getYamlPath(), b);
        }
        final boolean res;
        if (a.hasListValue()) {
            if (!a.getChildren().get(0).isObjectListItem()) {
                // special case: scalar lists are not merged and so should not be compared
                // but have assumed this case as match
                res = true;
                LOGGER.trace("\tscalars list can't be used for value matching: {}", a.getYamlPath());
            } else {
                res = b.hasChildren() && matchLists(a, b);
            }
        } else {
            // subtree matching
            if (a.hasChildren()) {
                // at least one property must match (and no different values detected)
                res = b.hasChildren() && matchSubtrees(a, b) > 0;
            } else {
                // direct value matching
                res = a.getIdentityValue().equals(b.getIdentityValue());
            }
        }
        return res;
    }

    private static <T extends YamlLine<T>> Integer matchSubtrees(final T a, final T b) {
        LOGGER.trace("\tmatching subtrees for {} and {}", a, b);
        int matches = 0;
        // all props found in left subtree must match props in the right subtree
        // (left prop may not be found on the right, but at least one property must match)
        for (T aprop : a.getChildren()) {
            if (!aprop.isProperty()) {
                LOGGER.trace("\tnot a property, skipping: {} ({})", aprop.getYamlPath(), aprop);
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
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("\tmatches: {} ({})", aprop.getYamlPath(), aprop);
                        }
                    }
                }
            }
            // found at least one not matched property (different value)
            if (propFound && !match) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("\tnot matched: {} ({})", aprop.getYamlPath(), aprop);
                }
                return 0;
            }
        }
        return matches;
    }

    private static <T extends YamlLine<T>> boolean matchLists(final T a, final T b) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("\tmatching lists for {} and {}", a.getYamlPath(), b.getYamlPath());
        }
        // for lists at least one left item must be found in the right list (by analogy with properties)
        // assuming its not scalar lists (it must be detected before)
        for (T ait : a.getChildren()) {
            final T match = match(ait, b.getChildren());
            if (match != null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("\tfount list item {} match: {}", ait.getYamlPath(), match.getYamlPath());
                }
                return true;
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("\tno matched item matches found for list: {}", a.getYamlPath());
        }
        return false;
    }
}
