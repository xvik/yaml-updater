package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.profile.TestConfigurator
import ru.vyarus.yaml.updater.report.ReportPrinter
import ru.vyarus.yaml.updater.report.UpdateReport
import spock.lang.Specification
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 14.07.2021
 */
abstract class AbstractTest extends Specification {

    @TempDir File tempDir

    protected TestConfigurator createQuickTest(String content1, String content2) {
        File src = new File(tempDir, 'from.yml')
        src << content1

        File patch = new File(tempDir, 'to.yml')
        patch << content2

        return YamlUpdater.createTest(src.absolutePath, patch.absolutePath)
    }

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
