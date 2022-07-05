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
import ru.vyarus.yaml.updater.report.ReportPrinter;
import ru.vyarus.yaml.updater.report.UpdateReport;
import ru.vyarus.yaml.updater.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        subparser.addArgument("--backup-dir")
                .dest("backupDir")
                .type(Arguments.fileType())
                .help("Directory to store backup in");

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

        subparser.addArgument("-s", "--non-strict")
                .dest("strict")
                .action(Arguments.storeFalse())
                .setDefault(true)
                .help("Don't fail if specified properties file does not exists");

        subparser.addArgument("-i", "--verbose")
                .dest("verbose")
                .action(Arguments.storeTrue())
                .setDefault(false)
                .help("Show debug logs");

        subparser.addArgument("--dry-run")
                .dest("dryrun")
                .action(Arguments.storeTrue())
                .setDefault(false)
                .help("Test run without file modification");
    }

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {
        final File current = namespace.get("file");
        final InputStream update = prepareTargetFile(namespace.get("update"));
        final boolean backup = namespace.get("backup");
        final File backupDir = namespace.get("backupDir");
        final boolean validate = namespace.get("validate");
        final List<String> delete = namespace.getList("delete");
        final boolean strict = namespace.get("strict");
        final Map<String, String> env = prepareEnv(namespace.getList("env"), strict);
        final boolean verbose = namespace.get("verbose");
        final boolean dryrun = namespace.get("dryrun");

        // logging is configured to WARN level by default, use direct output instead
        System.out.println("Updating configuration: " + current.getAbsolutePath());

        if (verbose) {
            enableDebugLogs();
        }

        final UpdateReport report = YamlUpdater.create(current, update)
                .backup(backup)
                .backupDir(backupDir)
                .deleteProps(delete != null ? delete.toArray(new String[]{}) : null)
                .validateResult(validate)
                .vars(env)
                .dryRun(dryrun)
                .update();

        System.out.println("\n" + ReportPrinter.print(report));

        if (dryrun && report.isConfigChanged()) {
            System.out.println(ReportPrinter.printDryRunResult(report));
        }
    }

    private InputStream prepareTargetFile(final String path) {
        final InputStream in = FileUtils.findFile(path);
        if (in == null) {
            throw new IllegalArgumentException("Update file not found: " + path);
        }
        return in;
    }

    @SuppressWarnings({"PMD.SystemPrintln", "PMD.CognitiveComplexity"})
    private Map<String, String> prepareEnv(final List<String> envList, final boolean strict) {
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
                    if (!FileUtils.loadProperties(env, res)) {
                        if (strict) {
                            throw new IllegalArgumentException("Variables file not found: " + env);
                        } else {
                            System.out.println("Ignore not found variables file: " + env);
                        }
                    }
                }
            }
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
