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

    protected String print(UpdateReport report) {
        def res = ReportPrinter.print(report)
        println res
        return res
        // size is different on win and lin
                .replaceAll("([^\\d])$report.beforeSize ", '$1300 ')
                .replaceAll("([^\\d])$report.afterSize ", '$1300 ')
                .replaceAll("([^\\d])$report.updateSize ", '$1300 ')
    }
}
