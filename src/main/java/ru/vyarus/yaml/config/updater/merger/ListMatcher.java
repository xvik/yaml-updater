package ru.vyarus.yaml.config.updater.merger;

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.model.YamlLine;

import java.util.*;

/**
 * @author Vyacheslav Rusakov
 * @since 06.06.2021
 */
public class ListMatcher {

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
                T cnd = it.next();
                boolean match = false;
                boolean propFound = false;
                for (T uprop : cnd.getChildren()) {
                    if (prop.getKey().equals(uprop.getKey())) {
                        propFound = true;
                        if (matches(prop, uprop)) {
                            match = true;
                            matchedItems.add(cnd.getLineNum());
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
        // subtree matching
        if (a.hasChildren()) {
            if (!b.hasChildren()) {
                return false;
            }
            int matches = 0;
            // all props found in left subtree must match props in the right subtree
            // (left prop may not be found on the right, but at least one property must match)
            for(T aprop: a.getChildren()) {
                boolean propFound = false;
                boolean match = false;
                for(T bprop: b.getChildren()) {
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

    private static class YamlListNode<T> extends YamlLine {
        public final YamlNode identity;
        // empty dash line or first property just after dash
        public boolean emptyDash;
        // object item or scalar
        public boolean object;

        // used during items matching to count how many properties match
        public int propsMatched;

        public YamlListNode(final YamlNode item) {
            // line number is unique identity for list item
            super(null, item.getPadding(), item.getLineNum());
            this.identity = item;
            this.object = item.hasChildren() && item.getChildren().get(0).isProperty();
            this.emptyDash = this.object && !item.isListItemWithProperty();
        }

        @Override
        public String getIdentityValue() {
            throw new UnsupportedOperationException("Fake node");
        }

        @Override
        public String toString() {
            return "(" + (object ? "object " + getChildren().size() : "scalar") + " | " + (emptyDash ? "empty dash" : "inline") + ") "
                    + identity.getLineNum() + ": " + identity;
        }
    }
}
