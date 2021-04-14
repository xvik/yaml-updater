package ru.vyarus.yaml.config.updater.merger;

import java.io.File;
import java.util.Map;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class MergerConfig {

    private File target;
    private boolean backup;
    private File source;
    private File deleteProps;
    // variables to apply to fresh config placeholders (adopt config to exact environment)
    private Map<String, String> env;

    private MergerConfig() {
    }

    public File getTarget() {
        return target;
    }

    public boolean isBackup() {
        return backup;
    }

    public File getSource() {
        return source;
    }

    public File getDeleteProps() {
        return deleteProps;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public static Builder builder(File target, File source) {
        return new Builder(target, source);
    }

    public static class Builder {
        private MergerConfig config = new MergerConfig();

        public Builder(File target, File source) {
            config.target = target;
            config.source = source;
        }

        public Builder backup(boolean backup) {
            config.backup = backup;
            return this;
        }

        public Builder deleteProps(File deletesFile) {
            config.deleteProps = deletesFile;
            return this;
        }

        public Builder envVars(Map<String, String> env) {
            config.env = env;
            return this;
        }

        public MergerConfig build() {
            return config;
        }
    }
}
