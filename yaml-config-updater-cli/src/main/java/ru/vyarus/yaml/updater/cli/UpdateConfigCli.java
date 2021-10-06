package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import ru.vyarus.yaml.updater.YamlUpdater;
import ru.vyarus.yaml.updater.report.ReportPrinter;
import ru.vyarus.yaml.updater.report.UpdateReport;

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
@Command(name = UpdateConfigCli.CMD_NAME, mixinStandardHelpOptions = true,
        description = "Update yaml configuration file from new file",
        versionProvider = ManifestVersionProvider.class)
public class UpdateConfigCli implements Callable<Void> {

    public static final String CMD_NAME = "update-config";

    @Parameters(index = "0", paramLabel = "CONFIG",
            description = "Path to updating configuration file (might not exist)")
    private File current;

    @Parameters(index = "1", paramLabel = "UPDATE",
            description = "Path to new configuration file or any URL")
    private String update;

    @Option(names = {"-b", "--no-backup"}, paramLabel = "BACKUP",
            description = "Don't create backup before configuration update")
    private boolean backup;

    @Option(names = {"-d", "--delete-path"}, arity = "1..*", paramLabel = "DELETE",
            description = "Delete properties from the current config before update")
    private List<String> removePaths;

    @Option(names = {"-e", "--env"}, arity = "1..*", paramLabel = "ENV",
            description = "Variables to replace (name=value) or path(s) to properties file with variables")
    private List<String> env;

    @Option(names = {"-v", "--no-validate"}, paramLabel = "VALID",
            description = "Don't validate the resulted configuration")
    private boolean valid;

    @Option(names = {"-i", "--verbose"}, paramLabel = "VERBOSE",
            description = "Show debug logs")
    private boolean verbose;

    @Option(names = "--dry-run", paramLabel = "DRYRUN",
            description = "Test run without file modification")
    private boolean dryrun;

    @Spec
    private CommandSpec spec;

    @Override
    @SuppressWarnings({"PMD.SystemPrintln", "checkstyle:MultipleStringLiterals"})
    public Void call() throws Exception {
        final InputStream target = resoleFile(update, "update");
        final Map<String, String> env = prepareEnv();

        enableLogs();

        System.out.println("Updating configuration: " + current.getAbsolutePath());

        final UpdateReport report = YamlUpdater.create(current, target)
                .backup(!backup)
                .deleteProps(removePaths != null ? removePaths.toArray(new String[0]) : null)
                .validateResult(!valid)
                .envVars(env)
                .dryRun(dryrun)
                .update();

        System.out.println("\n" + ReportPrinter.print(report));

        if (dryrun && report.isConfigChanged()) {
            System.out.println("\n---------------------------------------------------------- \n"
                    + "   Merged configuration (NOT SAVED): \n"
                    + "---------------------------------------------------------- \n\n"
                    + report.getDryRunResult() + "\n\n"
                    + "---------------------------------------------------------- \n\n");
        }

        return null;
    }

    public static void main(final String[] args) {
        String[] arg = args;
        // support usage like in help message to avoid errors
        if (arg.length > 0 && CMD_NAME.equals(arg[0])) {
            arg = new String[args.length - 1];
            System.arraycopy(args, 1, arg, 0, arg.length);
        }
        new CommandLine(new UpdateConfigCli()).execute(arg);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private InputStream resoleFile(final String name, final String desc) {
        if (name.contains(":")) {
            // url
            try {
                return new URL(name).openStream();
            } catch (IOException e) {
                throw new ParameterException(spec.commandLine(), "Invalid " + desc + " file url: " + name, e);
            }
        }
        final File file = new File(name);
        if (!file.exists()) {
            throw new ParameterException(spec.commandLine(), "Invalid " + desc + " file (does not exists): " + name);
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
                if (idx >= 0) {
                    // direct variable
                    final String name = env.substring(0, idx).trim();
                    if (name.isEmpty()) {
                        throw new ParameterException(spec.commandLine(), "Invalid variable declaration: " + env);
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
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            final Properties props = new Properties();
            props.load(reader);

            for (Object key : props.keySet()) {
                final String name = String.valueOf(key);
                final String value = props.getProperty(name);
                res.put(name, value == null ? "" : value);
            }
        } catch (Exception ex) {
            throw new ParameterException(spec.commandLine(), "Invalid variables file: " + path + " ("
                    + ex.getMessage() + ")", ex);
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
