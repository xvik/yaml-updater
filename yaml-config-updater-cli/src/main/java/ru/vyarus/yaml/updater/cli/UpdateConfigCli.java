package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import ru.vyarus.yaml.updater.YamlUpdater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * @author Vyacheslav Rusakov
 * @since 20.07.2021
 */
@Command(name = "update-config", mixinStandardHelpOptions = true,
        description = "Update yaml configuration file from new file")
public class UpdateConfigCli implements Callable<Void> {

    @Parameters(index = "0", paramLabel = "CONFIG",
            description = "Path to updating configuration file")
    File current;

    @Parameters(index = "1", paramLabel = "UPDATE",
            description = "Path to new configuration file or any URL")
    String update;

    @Option(names = {"-b", "--no-backup"}, paramLabel = "BACKUP",
            description = "Don't create backup before configuration update")
    boolean backup;

    @Option(names = {"-d", "--delete-paths"}, arity = "1..*", paramLabel = "DELETE",
            description = "Delete properties from the current config before update")
    List<String> removePaths;

    @Option(names = {"-e", "--env"}, arity = "1..*", paramLabel = "ENV",
            description = "Variables to replace (name=value) or path(s) to properties file with variables")
    List<String> env;

    @Option(names = {"-v", "--no-validate"}, paramLabel = "VALID",
            description = "Don't validate the resulted configuration")
    boolean valid;

    @Option(names = {"-i", "--verbose"}, paramLabel = "VERBOSE",
            description = "Show debug logs")
    boolean verbose;

    @Spec
    CommandSpec spec;

    @Override
    public Void call() throws Exception {
        final InputStream target = resoleFile(update, "update");
        final Map<String, String> env = prepareEnv();

        enableLogs();

        System.out.println("Updating configuration: " + current.getAbsolutePath());

        YamlUpdater.create(current, target)
                .backup(!backup)
                .deleteProps(removePaths != null ? removePaths.toArray(new String[0]) : null)
                .validateResult(!valid)
                .envVars(env)
                .update();

        System.out.println("\nConfiguration successfully updated");

        return null;
    }

    public static void main(final String[] args) {
        new CommandLine(new UpdateConfigCli()).execute(args);
    }

    private InputStream resoleFile(String name, String desc) {
        if (name.contains(":")) {
            // url
            try {
                return new URL(name).openStream();
            } catch (IOException e) {
                throw new ParameterException(spec.commandLine(), "Invalid " + desc + " file url: " + name
                        + " (" + e.getMessage() + ")");
            }
        }
        final File file = new File(name);
        if (!file.exists()) {
            throw new ParameterException(spec.commandLine(), desc + " file does not exists: " + name);
        }
        try {
            return Files.newInputStream(file.toPath());
        } catch (IOException e) {
            throw new ParameterException(spec.commandLine(), "Invalid " + desc + " file: " + file.getAbsolutePath(), e);
        }
    }

    private Map<String, String> prepareEnv() {
        // always included environment vars
        final Map<String, String> res = new HashMap<>(System.getenv());

        if (env != null) {
            for (String env : env) {
                final int idx = env.indexOf('=');
                if (idx > 0) {
                    // direct variable
                    final String name = env.substring(0, idx).trim();
                    if (name.isEmpty()) {
                        throw new IllegalArgumentException("Invalid variable declaration: " + env);
                    }
                    final String value = env.substring(idx + 1).trim();
                    res.put(name, value);
                } else {
                    // properties file
                    loadVarsFile(env, res);
                }
            }
        }
        return res;
    }

    private void loadVarsFile(final String path, final Map<String, String> res) {
        final InputStream in = resoleFile(path, "variables");
        if (in != null) {
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                final Properties props = new Properties();
                props.load(reader);

                for (Object key : props.keySet()) {
                    final String name = String.valueOf(key);
                    final String value = props.getProperty(name);
                    res.put(name, value == null ? "" : value);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read variables from: " + path, ex);
            }
        } else {
            throw new IllegalArgumentException("Variables file not found: " + path);
        }
    }

    private void enableLogs() {
        // important to call before simplelogger initialization
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        if (verbose) {
            System.setProperty("org.slf4j.simpleLogger.log.ru.vyarus.yaml", "debug");
        }
    }
}
