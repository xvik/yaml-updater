package ru.vyarus.yaml.updater.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Environment variables could be used to personalize updating yaml file. For example:
 * <pre>
 * some:
 *    - prop: #{var}
 * </pre>
 * <p>
 * Such syntax allows yaml parser to treat variable as a comment (if it would not be replaced). Also, variables
 * in format {@code ${va}} are often used directly in the configuration (to be replaced at runtime with environment
 * variables).
 * <p>
 * Variable names are case-sensitive. Unknown variables are not replaced. Null values replaced with empty.
 *
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
public class EnvSupport {
    private final Logger logger = LoggerFactory.getLogger(EnvSupport.class);

    private final String prefix;
    private final String postfix;
    private final Map<String, String> env;

    private final Map<String, String> applied = new HashMap<>();

    public EnvSupport(final Map<String, String> env) {
        // non standard vars identity used because:
        // - real config could rely on environment variables
        // - not processed file would remain valid yaml because placeholder would be interpreted as comment
        this("#{", "}", env);
    }

    public EnvSupport(final String prefix,
                       final String postfix,
                       final Map<String, String> env) {
        if (prefix == null) {
            throw new IllegalArgumentException("Variable prefix required");
        }
        if (postfix == null) {
            throw new IllegalArgumentException("Variable postfix required");
        }
        this.prefix = prefix;
        this.postfix = postfix;
        this.env = env;
    }

    /**
     * Replace variables in the provided text.
     *
     * @param text text to process
     * @return text with replaced known variables
     */
    public String apply(final String text) {
        if (text == null || text.isEmpty() || env == null || env.isEmpty()) {
            return text;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Replacing variables in format '{}name{}' from: \n{}", prefix, postfix,
                    env.entrySet().stream()
                            .map(entry -> "    " + entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining("\n")));
        }
        return replace(text);
    }

    /**
     * @return applied variables in the latest execution or empty map
     */
    public Map<String, String> getApplied() {
        return new HashMap<>(applied);
    }

    @SuppressWarnings("checkstyle:IllegalIdentifierName")
    private String replace(final String text) {
        applied.clear();
        String res = text;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            final String var = prefix + entry.getKey() + postfix;
            String value = entry.getValue();
            if (value == null) {
                value = "";
            }

            final Matcher matcher = Pattern.compile(var, Pattern.LITERAL).matcher(res);
            final String replacement = Matcher.quoteReplacement(value);
            int cntr = 0;
            while (matcher.find()) {
                cntr++;
            }
            if (cntr > 0) {
                logger.debug("    {} ({}) replaced with: {}", var, cntr, replacement);
                res = matcher.replaceAll(replacement);
                applied.put(entry.getKey(), value);
            }
        }
        return res;
    }
}
