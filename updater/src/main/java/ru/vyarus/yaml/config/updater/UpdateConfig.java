package ru.vyarus.yaml.config.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Update configuration. Required current config and updating file. Optionally, environment variables could be
 * provided (to replace in updating file). To override existing values (or to remove outdated paths) specify yaml
 * paths to remove in old file before update (note that yaml path elements split with '/' because yaml property
 * could contain dots).
 *
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public final class UpdateConfig {

    private File current;
    private boolean backup;
    // not file to allow loading from classpath (jar) or any other location
    private String update;
    private List<String> deleteProps = Collections.emptyList();
    // variables to apply to fresh config placeholders (adopt config to exact environment)
    private Map<String, String> env = Collections.emptyMap();
    private boolean validateResult = true;

    private UpdateConfig() {
    }

    /**
     * Yaml updater configurator.
     *
     * @param current current configuration file
     * @param update update file
     * @return builder for config construction
     */
    public static Configurator configureUpdate(final File current, final InputStream update) {
        return new Configurator(current, update);
    }

    /**
     * @return current config that must be updated
     */
    public File getCurrent() {
        return current;
    }

    /**
     * @return true to save current config backup after update
     */
    public boolean isBackup() {
        return backup;
    }

    /**
     * Configuration file may contain placeholders ({@link #getEnv()}).
     * Only properties not found in the current config would be added (for properties remove
     * see {@link #getDeleteProps()}).
     *
     * @return new configuration to update from
     */
    public String getUpdate() {
        return update;
    }

    /**
     * NOTE: '/' used for property separation because in yaml property name could contain dot.
     *
     * @return properties to remove in current file (would be replaced if provided in new file)
     */
    public List<String> getDeleteProps() {
        return deleteProps;
    }

    /**
     * Variables for replacing placeholders in update file {@code #{var}}.
     * Variables searched without counting yaml semantics.
     * Used to adopt configuration to the target environment (especially useful for the first installation).
     *
     * @return variables to replace in updating file
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * @return true to validate result against old and new file trees (to make sure all old values preserved and new
     * values added)
     */
    public boolean isValidateResult() {
        return validateResult;
    }


    /**
     * Updater configurator.
     */
    public static final class Configurator {
        private final Logger logger = LoggerFactory.getLogger(Configurator.class);
        private final UpdateConfig config = new UpdateConfig();

        private Configurator(final File current, final InputStream update) {
            if (current == null) {
                throw new IllegalArgumentException("Current config file not specified");
            }
            config.current = current;

            if (update == null) {
                throw new IllegalArgumentException("New config file not specified");
            }
            final String text = read(update);
            if (text.isEmpty()) {
                throw new IllegalArgumentException("New config file is empty");
            }
            config.update = text;
        }

        /**
         * @param backup true to do backup of configuration before update
         * @return builder instance for chained calls
         */
        public Configurator backup(final boolean backup) {
            config.backup = backup;
            return this;
        }

        /**
         * IMPORTANT: properties must be separated with "/" and not "."! This is important because dot is allowed
         * character in property name!
         * <p>
         * Yaml path may include list values with syntax: prop/sublist[0]/foo. It would match first item of list
         * prop/sublist and select foo item property.
         * <pre>
         * prop:
         *    sublist:
         *      - foo: 1
         *        bar: 2
         * </pre>
         *
         * @param deleteProps yaml paths to delete in old file (would be replaced with props from new file)
         * @return builder instance for chained calls
         */
        public Configurator deleteProps(final String... deleteProps) {
            config.deleteProps = Arrays.asList(deleteProps);
            return this;
        }

        /**
         * Disables merged file validation (checks that all old values remains and new values added). This might be
         * useful only in case of bugs in validation logic (comparing yaml trees). When validation is disabled, merged
         * file is still parsed with snakeyaml to make sure it's readable.
         *
         * @return builder instance for chained calls
         */
        public Configurator noResultValidation() {
            config.validateResult = false;
            return this;
        }

        /**
         * Variables use special syntax {@code #{name}} because with it yaml file still remains valid (variable treated
         * as comment).
         *
         * @param env variables to replace in updating file
         * @return builder instance for chained calls
         */
        public Configurator envVars(final Map<String, String> env) {
            config.env = env;
            return this;
        }

        /**
         * @return configured yaml updater instance
         */
        public YamlUpdater create() {
            return new YamlUpdater(config);
        }

        @SuppressWarnings("PMD.UseTryWithResources")
        private String read(final InputStream in) {
            try {
                return new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"))
                        .trim();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.debug("Failed to close stream", e);
                }
            }
        }
    }
}
