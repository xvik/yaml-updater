package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.report.ReportPrinter
import ru.vyarus.yaml.updater.report.UpdateReport
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.07.2021
 */
abstract class AbstractTest extends Specification {

    protected String unifyString(String input) {
        return input
        // cleanup win line break for simpler comparisons
                .replace("\r", '')
    }

    protected String print(UpdateReport report, long srcSize, long size) {
        return ReportPrinter.print(report)
        // size is different on win and lin
                .replaceAll("([^\\d])$srcSize ", '$1300 ')
                .replaceAll("([^\\d])$size ", '$1301 ')
    }
}
