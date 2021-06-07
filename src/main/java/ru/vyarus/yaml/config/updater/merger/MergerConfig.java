package ru.vyarus.yaml.config.updater.merger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Merge configuration. Required current config and updating file. Optionally environment variables could be
 * provided (to replace in updating file) and special config with properties to remove (only new properties are
 * copied from update config).
 *
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class MergerConfig {

    private File current;
    private boolean backup;
    private File update;
    private List<String> deleteProps;
    // variables to apply to fresh config placeholders (adopt config to exact environment)
    private Map<String, String> env;
    private boolean validateResult = true;

    private MergerConfig() {
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
     * Only properties not found in current config would be added (for properties remove see {@link #getDeleteProps()}).
     *
     * @return new configuration to update from
     */
    public File getUpdate() {
        return update;
    }

    /**
     * @return properties to remove in current file (would be replaced if provided in new file)
     */
    public List<String> getDeleteProps() {
        return deleteProps;
    }

    /**
     * Variables replaces as {@code ${var}}. Variables are searched without counting yaml semantics.
     * Used to adopt universal configuration to the target environment (especially useful for the first installation).
     *
     * @return variables to replace in updating file
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     *
     * @return true to validate result against old and new file trees (to make sure all old values preserved and new
     *  values added)
     */
    public boolean isValidateResult() {
        return validateResult;
    }

    public static Builder builder(final File current, final File update) {
        return new Builder(current, update);
    }

    public static class Builder {
        private final MergerConfig config = new MergerConfig();

        public Builder(File current, File update) {
            config.current = current;
            config.update = update;
        }

        public Builder backup(final boolean backup) {
            config.backup = backup;
            return this;
        }

        public Builder deleteProps(final String... deleteProps) {
            config.deleteProps = Arrays.asList(deleteProps);
            return this;
        }

        /**
         * Disables merged file validation (all old values remain and new values added). This might be useful
         * only in case of bugs in validation logic (comparing yaml trees). When validation is disabled, merged
         * file is still parsed with snakeyaml to make sure its readable.
         *
         * @return builder instance for chained calls
         */
        public Builder noResultValidation() {
            config.validateResult = false;
            return this;
        }

        public Builder envVars(final Map<String, String> env) {
            config.env = env;
            return this;
        }

        public MergerConfig build() {
            if (config.getCurrent() == null) {
                throw new IllegalStateException("Current config file not specified");
            }
            if (config.getUpdate() == null) {
                throw new IllegalStateException("New config file not specified");
            }
            if (!config.getUpdate().exists()) {
                throw new IllegalStateException("New config file does not exists: "
                        + config.getUpdate().getAbsolutePath());
            }
            return config;
        }
    }
}
