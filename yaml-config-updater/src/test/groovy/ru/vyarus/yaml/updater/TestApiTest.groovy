package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.report.UpdateReport

/**
 * @author Vyacheslav Rusakov
 * @since 07.10.2021
 */
class TestApiTest extends AbstractTest {

    def "Test run with reports"() {

        when: "merging"

        def current = new File(getClass().getResource('/merge/simple.yml').toURI())
        long curSize = current.size()
        def report = YamlUpdater.createTest(
                '/merge/simple.yml',
                '/merge/simple_upd.yml')
                .update()

        then: "updated"
        report.dryRun
        // files size would be different here because afterSize obtained with File.length() which may vary between OS
        report.dryRunResult.split(System.lineSeparator()).size() == report.afterLinesCnt
        report.beforeSize != report.afterSize
        report.beforeLinesCnt != report.afterLinesCnt
        report.afterSize != report.updateSize
        report.afterLinesCnt != report.updateLines
    }

    def "Check dry run disable"() {

        when: "disabling dry run"
        YamlUpdater.createTest(
                '/merge/simple.yml',
                '/merge/simple_upd.yml')
                .dryRun(false)
                .update()

        then: "fail"
        thrown(IllegalArgumentException)
    }

    def "Check reports disable"() {

        when: "disabling reports"
        UpdateReport report = YamlUpdater.createTest(
                '/merge/simple.yml',
                '/merge/simple_upd.yml')
                .printReport(false)
                .printResult(false)
                .update()

        then: "only visual confirmation"
        report.dryRun
    }
}
