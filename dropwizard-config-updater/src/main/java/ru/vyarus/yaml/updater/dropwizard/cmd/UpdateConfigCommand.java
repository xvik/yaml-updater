package ru.vyarus.yaml.updater.dropwizard.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.dropwizard.cli.Command;
import io.dropwizard.logging.LoggingUtil;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import ru.vyarus.yaml.updater.YamlUpdater;

import java.io.File;
import java.io.FileNotFoundException;
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

/**
 * Update config dropwizard command. It is required to specify complete or relative path to application configuration
 * and absolute path or relative path or classpath path for updating file.
 *
 * @author Vyacheslav Rusakov
 * @since 17.07.2021
 */
@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class UpdateConfigCommand extends Command {

    public UpdateConfigCommand() {
        super("update-config", "Update configuration file from new file");
    }

    @Override
    public void configure(final Subparser subparser) {
        subparser.addArgument("file")
                .required(true)
                .type(Arguments.fileType())
                .help("Path to updating configuration file (might not exist)");

        subparser.addArgument("update")
                .required(true)
                .help("Path to new configuration file. Could also be a classpath path or any URL.");

        subparser.addArgument("-b", "--no-backup")
                .dest("backup")
                .action(Arguments.storeFalse())
                .setDefault(true)
                .help("Don't create backup before configuration update");

        subparser.addArgument("-d", "--delete-path")
                .dest("delete")
                .nargs("+")
                .help("Delete properties from the current config before update");

        subparser.addArgument("-e", "--env")
                .dest("env")
                .nargs("+")
                .help("Variables to replace (name=value) or path(s) to properties file with variables "
                        + "(could also be a classpath path or any URL)");

        subparser.addArgument("-v", "--no-validate")
                .dest("validate")
                .action(Arguments.storeFalse())
                .setDefault(true)
                .help("Don't validate the resulted configuration");

        subparser.addArgument("-i", "--verbose")
                .dest("verbose")
                .action(Arguments.storeTrue())
                .setDefault(false)
                .help("Show debug logs");
    }

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        final File current = namespace.get("file");
        final InputStream update = prepareTargetFile(namespace.get("update"));
        final boolean backup = namespace.get("backup");
        final boolean validate = namespace.get("validate");
        final List<String> delete = namespace.getList("delete");
        final Map<String, String> env = prepareEnv(namespace.getList("env"));
        final boolean verbose = namespace.get("verbose");

        // logging is configured to WARN level by default, use direct output instead
        System.out.println("Updating configuration: " + current.getAbsolutePath());

        if (verbose) {
            enableDebugLogs();
        }

        YamlUpdater.create(current, update)
                .backup(backup)
                .deleteProps(delete != null ? delete.toArray(new String[]{}) : null)
                .validateResult(validate)
                .envVars(env)
                .update();

        System.out.println("\nConfiguration successfully updated");
    }

    private InputStream prepareTargetFile(final String path) {
        final InputStream in = findFile(path);
        if (in == null) {
            throw new IllegalArgumentException("Update file not found: " + path);
        }
        return in;
    }

    private Map<String, String> prepareEnv(final List<String> envList) {
        // always included environment vars
        final Map<String, String> res = new HashMap<>(System.getenv());

        if (envList != null) {
            for (String env : envList) {
                final int idx = env.indexOf('=');
                if (idx >= 0) {
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
        final InputStream in = findFile(path);
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

    private InputStream findFile(final String path) {
        InputStream res;
        // first check direct file
        final File file = new File(path);
        if (file.exists()) {
            try {
                res = Files.newInputStream(file.toPath());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read file: " + path, e);
            }
        } else if (path.indexOf(':') > 0) {
            // url
            try {
                res = new URL(path).openStream();
            } catch (FileNotFoundException ex) {
                res = null;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load file from url: " + path, e);
            }
        } else {
            // try to resolve in classpath
            res = UpdateConfigCommand.class.getResourceAsStream(path);
        }
        return res;
    }

    private void enableDebugLogs() {
        // re-configure default logging
        final Logger root = LoggingUtil.getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        // by default bootstrap would set threshold filter for WARN, see
        // io.dropwizard.logging.BootstrapLogging.bootstrap(..)
        root.iteratorForAppenders().next().clearAllFilters();
        root.setLevel(Level.WARN);
        LoggingUtil.getLoggerContext().getLogger(YamlUpdater.class.getPackage().getName()).setLevel(Level.DEBUG);
    }
}
