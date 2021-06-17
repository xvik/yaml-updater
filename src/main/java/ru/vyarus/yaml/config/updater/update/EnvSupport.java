package ru.vyarus.yaml.config.updater.update;

import java.util.Map;

/**
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
public class EnvSupport {

    public static String apply(final String text,
                               final Map<String, String> env) {
        // non standard vars identity used because:
        // - real config could rely on environment variables
        // - not processed file would remain valid yaml because placeholder would be interpreted as comment
        return apply(text, "#{", "}", env);
    }

    public static String apply(final String text,
                               final String prefix,
                               final String postfix,
                               final Map<String, String> env) {
        if (text == null || text.isEmpty() || env == null || env.isEmpty()) {
            return text;
        }
        if (prefix == null) {
            throw new IllegalArgumentException("Variable prefix required");
        }
        if (postfix == null) {
            throw new IllegalArgumentException("Variable postfix required");
        }

        String res = text;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String var = prefix + entry.getKey() + postfix;
            String value = entry.getValue();
            if (value == null) {
                value = "";
            }
            res = res.replace(var, value);
        }
        return res;
    }
}
