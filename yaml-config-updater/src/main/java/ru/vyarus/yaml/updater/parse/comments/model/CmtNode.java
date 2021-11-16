package ru.vyarus.yaml.updater.parse.comments.model;

import ru.vyarus.yaml.updater.parse.common.model.YamlLine;

import java.util.ArrayList;
import java.util.List;

/**
 * One or multiple lines in yaml file. Usually represent property ({@code something: val}), its value and comment.
 * Everything before property that is not a property assumed to be its comment (important to recover comments as-is;
 * actually it doesn't matter what property they belong actually, it's important to not change structure).
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
public class CmtNode extends YamlLine<CmtNode> {

    // key may be null for comment only block (could go last in file)

    // yaml property name might be quoted and contain escaped quotes (''): key must be unquoted to avoid comparison
    // mistakes (snakeyaml would do it automatically, plus, same property may be quoted in source file and not quoted
    // in update file (or vice versa))
    // Preserving original property declaration to be able to write it exactly as-is
    private String sourceKey;

    // important: value might contain comment (right comment)!
    // so even for object declaration value may exist (containing just comment)
    private List<String> value = new ArrayList<>();
    // node comment is everything above before previous node
    // using list to avoid dealing with line separators
    private final List<String> topComment = new ArrayList<>();

    // this value is set from structure parser
    private String parsedValue;

    // special marker for added nodes during merge
    private boolean addedNode;

    public CmtNode(final CmtNode root, final int padding, final int lineNum) {
        super(root, padding, lineNum);
    }

    @Override
    public void setKey(final String key) {
        sourceKey = key;
        String cleanKey = key;
        if (key != null) {
            final char first = key.charAt(0);
            if (first == '"' || first == '\'') {
                if (key.charAt(key.length() - 1) != first) {
                    throw new IllegalStateException(
                            "Quoted property must start and end with the same quote symbol: [" + key + "]");
                }
                cleanKey = cleanKey.substring(1, cleanKey.length() - 1)
                        // replace possible escaped quotes
                        .replace("''", "'");
            }
        }
        super.setKey(cleanKey);
    }

    /**
     * Original yaml file could contain quoted key, but {@link #getKey()} would always return unquoted key to avoid
     * comparison mistakes. Use this method to get original key before writing it back to file (method ONLY for
     * correct node writing).
     *
     * @return original key representation (quoted, if it was originally quoted in the file)
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * @return property value (multiple lines for multi-line values) or empty list
     */
    public List<String> getValue() {
        return value;
    }

    /**
     * Register property value.
     *
     * @param value property value
     */
    public void setValue(final List<String> value) {
        this.value = value;
    }

    /**
     * NOTE: If multiple comments above property, separated by blank lines - it all would be assumed as property
     * comment (everything is remembered to re-create exactly the same file).
     *
     * @return comment (multiple lines) above property or empty list if no comment
     */
    public List<String> getTopComment() {
        return topComment;
    }

    /**
     * NOTE: such value may not be set if comments parser used directly (e.g. in tests).
     *
     * @return value from snakeyaml parser or null
     */
    public String getParsedValue() {
        return parsedValue;
    }

    /**
     * Property value, parsed by snakeyaml parser. Used for list items matching (see {@link #getIdentityValue()}).
     *
     * @param parsedValue value from snakeyaml parser
     */
    public void setParsedValue(final String parsedValue) {
        this.parsedValue = parsedValue;
    }

    /**
     * @return true if property has comment above
     */
    public boolean hasComment() {
        return !getTopComment().isEmpty();
    }

    /**
     * @return true if node contains only comment without property (trailing file or subtree comment)
     */
    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    public boolean isCommentOnly() {
        return hasComment() && !isListItem() && getKey() == null && (getValue() == null || getValue().isEmpty());
    }

    /**
     * @return true if property has non empty value
     */
    public boolean hasValue() {
        return getIdentityValue() != null;
    }

    @Override
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public String getIdentityValue() {
        // if possible use value from snakeyaml
        // (normally only this branch should work)
        if (parsedValue != null) {
            return parsedValue;
        }
        // collect value from parsed lines, excluding whitespace and comments
        String res = "";
        for (String line : value) {
            if (line == null) {
                continue;
            }
            res += line.trim();
            final int cmt = res.indexOf('#');
            if (cmt > 0) {
                res = res.substring(0, cmt).trim();
            }
        }
        return res.isEmpty() ? null : res;
    }

    /**
     * Mark node as added during merge process.
     *
     * @param addedNode true to mark node as added
     */
    public void setAddedNode(final boolean addedNode) {
        this.addedNode = addedNode;
    }

    /**
     * @return true indicates added node during merge process (including lists update)
     */
    public boolean isAddedNode() {
        return addedNode;
    }

    @Override
    public String toString() {
        if (isCommentOnly()) {
            return topComment.get(0);
        }
        final String value = hasValue() ? this.value.get(0) : "";
        return isListItem() ? "- " + value
                : getSourceKey() + ":" + value;
    }
}
