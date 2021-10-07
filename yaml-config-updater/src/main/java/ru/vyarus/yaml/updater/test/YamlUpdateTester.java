package ru.vyarus.yaml.updater.test;

import ru.vyarus.yaml.updater.YamlUpdater;
import ru.vyarus.yaml.updater.listen.UpdateListener;
import ru.vyarus.yaml.updater.report.ReportPrinter;
import ru.vyarus.yaml.updater.report.UpdateReport;
import ru.vyarus.yaml.updater.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to simplify testing updates: it could search file on fs, in classpath or as url. Executes updater in
 * dry run mode (to avoid modifications). In any case, temp file used as original config (with copied content), so
 * nothing could be changed in any case.
 * <p>
 * By default, prints update report and complete merged file (for visual validation).
 * <p>
 * Target use case: keep old config somewhere in test classpath and compare it with the newer config version
 * to make sure production migration will not fail.
 *
 * @author Vyacheslav Rusakov
 * @since 06.10.2021
 */
@SuppressWarnings("PMD.SystemPrintln")
public final class YamlUpdateTester {

    private final String configPath;
    private final File config;
    private final InputStream update;

    private boolean report = true;
    private boolean result = true;
    private final Map<String, String> envVars = new HashMap<>();

    private boolean validate = true;
    private UpdateListener listener;

    private YamlUpdateTester(final String configPath, final InputStream target) {
        this.configPath = configPath;
        // updater api requires file, so creating tmp file
        this.config = FileUtils.copyToTempFile(configPath);
        this.update = target;
    }

    /**
     * @param config updating config path (fs, classpath or url)
     * @param update target config path (fs, classpath or url)
     * @return updater instance for configuration
     */
    public static YamlUpdateTester create(final String config, final String update) {
        final InputStream target = FileUtils.findExistingFile(update);
        final YamlUpdateTester res = new YamlUpdateTester(config, target);
        System.out.println("Source config: " + config);
        System.out.println("Update config: " + update);
        return res;
    }

    /**
     * Enabled by default.
     *
     * @param printReport print update summary report
     * @return tester instance for chained calls
     */
    public YamlUpdateTester printReport(final boolean printReport) {
        this.report = printReport;
        return this;
    }

    /**
     * Enabled by default. Print file only if it's changed during merge.
     *
     * @param printResult print entire merged file
     * @return tester instance for chained calls
     */
    public YamlUpdateTester printResult(final boolean printResult) {
        this.result = printResult;
        return this;
    }

    /**
     * Adds variable for source config substitution. May be called multiple times.
     *
     * @param name  variable name
     * @param value variable value
     * @return tester instance for chained calls
     * @see ru.vyarus.yaml.updater.UpdateConfig.Configurator#vars(java.util.Map)
     */
    @SuppressWarnings("checkstyle:IllegalIdentifierName")
    public YamlUpdateTester var(final String name, final String value) {
        this.envVars.put(name, value);
        return this;
    }

    /**
     * Add variables for source config substitution. May be called multiple times.
     *
     * @param vars variables map (may be null)
     * @return tester instance for chained calls
     * @see ru.vyarus.yaml.updater.UpdateConfig.Configurator#vars(java.util.Map)
     */
    public YamlUpdateTester vars(final Map<String, String> vars) {
        if (vars != null) {
            this.envVars.putAll(vars);
        }
        return this;
    }

    /**
     * Add variables from properties file (from fs, classpath or url).
     *
     * @param path file path
     * @return tester instance for chained calls
     */
    public YamlUpdateTester vars(final String path) {
        if (!FileUtils.loadProperties(path, this.envVars)) {
            throw new IllegalArgumentException("Properties file not found: " + path);
        }
        return this;
    }

    /**
     * {@link ru.vyarus.yaml.updater.UpdateConfig.Configurator#validateResult(boolean)} option. May be used
     * in cases when validation fails due to internal validation bugs (workaround).
     *
     * @param validateResult validate result
     * @return tester instance for chained calls
     */
    public YamlUpdateTester validateResult(final boolean validateResult) {
        this.validate = validateResult;
        return this;
    }

    /**
     * {@link ru.vyarus.yaml.updater.UpdateConfig.Configurator#listen(ru.vyarus.yaml.updater.listen.UpdateListener)}
     * option. May be used in case when custom listener required for update process.
     *
     * @param listener listener instance
     * @return tester instance for chained calls
     */
    public YamlUpdateTester listen(final UpdateListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Merged file content would be stored in {@link UpdateReport#getDryRunResult()} for validation (because
     * original config is not modified).
     *
     * @return execute migration simulation
     */
    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public UpdateReport execute() {
        try {
            final UpdateReport report = YamlUpdater.create(config, update)
                    .vars(envVars)
                    .dryRun(true)
                    .validateResult(validate)
                    .listen(listener)
                    .update();

            if (this.report) {
                // hide temp file for more clear report
                final String render = ReportPrinter.print(report).replace(config.getAbsolutePath(), configPath);
                System.out.println("\n" + render);
            }
            if (result && report.isConfigChanged()) {
                System.out.println("\n---------------------------------------------------------- \n"
                        + "   Merged configuration (NOT SAVED): \n"
                        + "---------------------------------------------------------- \n\n"
                        + report.getDryRunResult() + "\n\n"
                        + "---------------------------------------------------------- \n\n");
            }

            return report;
        } finally {
            if (!config.delete()) {
                System.err.println("Can't delete tmp file: " + config.getAbsolutePath());
            }
        }
    }
}
