package ru.vyarus.yaml.updater.profile;

import ru.vyarus.yaml.updater.UpdateConfig;
import ru.vyarus.yaml.updater.report.ReportPrinter;
import ru.vyarus.yaml.updater.report.UpdateReport;
import ru.vyarus.yaml.updater.util.FileUtils;

/**
 * Utility to simplify testing updates: it could search file on fs, in classpath or as url. Executes updater in
 * dry run mode (to avoid modifications). Temp file is used as original config (with copied content), so
 * nothing could be changed in any case.
 * <p>
 * By default, prints update report and complete merged file (for visual validation).
 * <p>
 * Target use case: keep old config somewhere in test classpath and compare it with the newer config version
 * to make sure production migration will not fail.
 *
 * @author Vyacheslav Rusakov
 * @see ru.vyarus.yaml.updater.YamlUpdater#createTest(String, String)
 * @since 06.10.2021
 */
@SuppressWarnings("PMD.SystemPrintln")
public final class TestConfigurator extends UpdateConfig.Configurator<TestConfigurator> {

    private final String configPath;
    private final String updatePath;

    private boolean report = true;
    private boolean result = true;

    public TestConfigurator(final String config, final String update) {
        // updater api requires file, so creating tmp file
        super(FileUtils.copyToTempFile(config), FileUtils.findExistingFile(update));
        dryRun(true); // this would also disable backup
        this.configPath = config;
        this.updatePath = update;
    }

    /**
     * Enabled by default.
     *
     * @param printReport print update summary report
     * @return tester instance for chained calls
     */
    public TestConfigurator printReport(final boolean printReport) {
        this.report = printReport;
        return this;
    }

    /**
     * Enabled by default. Print file only if it's changed during merge.
     *
     * @param printResult print entire merged file
     * @return tester instance for chained calls
     */
    public TestConfigurator printResult(final boolean printResult) {
        this.result = printResult;
        return this;
    }

    @Override
    public TestConfigurator dryRun(final boolean dryRun) {
        if (!dryRun) {
            throw new IllegalArgumentException("Test run must be a dry run");
        }
        return super.dryRun(dryRun);
    }

    @Override
    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public UpdateReport update() {
        try {
            if (this.report) {
                System.out.println("Source config: " + configPath);
                System.out.println("Update config: " + updatePath);
            }

            final UpdateReport report = super.update();

            if (this.report) {
                // hide temp file for more clear report
                final String render = ReportPrinter.print(report)
                        .replace(config.getCurrent().getAbsolutePath(), configPath);
                System.out.println("\n" + render);
            }
            if (result && report.isConfigChanged()) {
                System.out.println(ReportPrinter.printDryRunResult(report));
            }

            return report;
        } finally {
            if (!config.getCurrent().delete()) {
                System.err.println("Can't delete tmp file: " + config.getCurrent().getAbsolutePath());
            }
        }
    }
}
