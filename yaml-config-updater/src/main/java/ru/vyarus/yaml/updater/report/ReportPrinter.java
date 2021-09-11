package ru.vyarus.yaml.updater.report;

import java.util.List;
import java.util.Map;

/**
 * Formats update report.
 *
 * @author Vyacheslav Rusakov
 * @since 31.08.2021
 */
@SuppressWarnings("checkstyle:MultipleStringLiterals")
public final class ReportPrinter {

    private ReportPrinter() {
    }

    /**
     * @param report report object
     * @return formatted update report
     */
    public static String print(final UpdateReport report) {
        final StringBuilder res = new StringBuilder();
        if (report.isConfigChanged()) {
            printHeader(report, res);

            if (!report.getAppliedVariables().isEmpty()) {
                res.append("\n\tApplied variables:\n");
                for (Map.Entry<String, String> entry : report.getAppliedVariables().entrySet()) {
                    res.append("\t\t").append(String.format("%-25s = %s", entry.getKey(), entry.getValue()))
                            .append('\n');
                }
            }

            if (report.getBeforeSize() == 0) {
                res.append("\n\tNew configuration copied as-is\n");
            } else if (report.getRemoved().isEmpty() && report.getAdded().isEmpty()) {
                res.append("\n\tOnly comments, order or formatting changed\n");
            } else {
                if (!report.getRemoved().isEmpty()) {
                    res.append("\n\tRemoved from old file:\n");
                    printChanges(report.getRemoved(), res);
                }
                if (!report.getAdded().isEmpty()) {
                    res.append("\n\tAdded from new file:\n");
                    printChanges(report.getAdded(), res);
                }
            }

            if (report.getBackup() != null) {
                res.append("\n\tBackup created: ").append(report.getBackup().getName()).append('\n');
            }
        } else {
            res.append("Configuration not changed: ").append(report.getConfig().getAbsolutePath()).append(" (");
            printSize(report.getBeforeSize(), report.getBeforeLinesCnt(), res);
            res.append(")\n");
        }
        return res.toString();
    }

    private static void printHeader(final UpdateReport report, final StringBuilder out) {
        if (report.getBeforeSize() > 0) {
            out.append("Configuration: ").append(report.getConfig().getAbsolutePath()).append(" (");
            printSize(report.getBeforeSize(), report.getBeforeLinesCnt(), out);
            out.append(")\n");
        } else {
            out.append("Not existing configuration: ").append(report.getConfig().getAbsolutePath()).append('\n');
        }

        out.append("Updated from source of ");
        printSize(report.getUpdateSize(), report.getUpdateLines(), out);

        out.append("\nResulted in ");
        printSize(report.getAfterSize(), report.getAfterLinesCnt(), out);
        out.append('\n');
    }

    private static void printSize(final long size, final int lines, final StringBuilder out) {
        out.append(size).append(" bytes, ").append(lines).append(" lines");
    }

    private static void printChanges(final List<UpdateReport.Pair> changes, final StringBuilder out) {
        for (UpdateReport.Pair change : changes) {
            out.append("\t\t").append(String.format("%-40s %s%s", change.getPath(), change.getValue(), '\n'));
        }
    }
}
