package ru.vyarus.yaml.config.updater.parse.comments.model;

import ru.vyarus.yaml.config.updater.parse.model.YamlLine;

import java.util.ArrayList;
import java.util.List;

/**
 * One or multiple lines in yaml file. Usually represent property ({@code something: val}), its value and comment.
 * Everything before property that is not a property assumed to be it's comment (important to recover comments as-is;
 * actually it doesn't matter what property they belong actually, its important to not change structure).
 * <p>
 * If there are only comment lines at the end of yaml file then special node would be used: comment without property.
 * <p>
 * For lists, parsed structure is a bit weird for objects: dashed property goes first and later object properties
 * are children of this value (so item object become split, but this simplifies parsing (node always one or more
 * lines)).
 * <p>
 * Value part for properties is parsed as-is, preserving possible in-line comments. For multi-line values, exact
 * lines would be stored (easier re-build file exactly as it was).
 *
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class YamlNode extends YamlLine<YamlNode> {

    // key may be null for comment only block (could go last in file)

    // important: value might contain comment (right comment)!
    // so even for object declaration value may exist (containing just comment)
    private List<String> value = new ArrayList<>();
    // node comment is everything above before previous node
    // using list to avoid dealing with line separators
    private final List<String> topComment = new ArrayList<>();
    // property commented (commented properties are searched by updating file structure, which is assuming to contain
    // all possible properties)
    private boolean commented;

    // this value is set from structure parser
    private String parsedValue;

    public YamlNode(final YamlNode root, final int padding, final int lineNum) {
        super(root, padding, lineNum);
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public List<String> getTopComment() {
        return topComment;
    }

    public boolean isCommented() {
        return commented;
    }

    public void setCommented(boolean commented) {
        this.commented = commented;
    }

    public String getParsedValue() {
        return parsedValue;
    }

    public void setParsedValue(String parsedValue) {
        this.parsedValue = parsedValue;
    }

    public boolean hasComment() {
        return !getTopComment().isEmpty();
    }

    public boolean isCommentOnly() {
        return hasComment() && !isListItem() && getKey() == null && (getValue() == null || getValue().isEmpty());
    }

    public boolean hasValue() {
        return getIdentityValue() != null;
    }

    @Override
    public String getIdentityValue() {
        // if possible use value from snakeyaml
        // (normally only this branch should work)
        if (parsedValue != null) {
            return parsedValue;
        }
        // collect value from parsed liens, excluding whitespace and comments
        String res = "";
        for (String line : value) {
            if (line == null) {
                continue;
            }
            res += line.trim();
            int cmt = res.indexOf("#");
            if (cmt > 0) {
                res = res.substring(0, cmt).trim();
            }
        }
        return res.isEmpty() ? null : res;
    }

    @Override
    public String toString() {
        if (isCommentOnly()) {
            return topComment.get(0);
        }
        final String value = hasValue() ? this.value.get(0) : "";
        if (isListItem()) {
            return "- " + value;
        } else {
            return getKey() + ": " + value;
        }
    }
}
