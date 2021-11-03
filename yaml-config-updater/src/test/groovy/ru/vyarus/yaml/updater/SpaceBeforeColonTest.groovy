package ru.vyarus.yaml.updater

/**
 * @author Vyacheslav Rusakov
 * @since 04.11.2021
 */
class SpaceBeforeColonTest extends AbstractTest {

    def "Check space before colon ignored"() {

        when: "merging file with space after property name"
        def report = YamlUpdater.createTest("/merge/whitespace.yml", "/merge/whitespace.yml")
                .update()

        then: "whitespace in names removed"
        unifyString(report.dryRunResult) == """name: value

name2: value

name3:    value
"""
    }
}
