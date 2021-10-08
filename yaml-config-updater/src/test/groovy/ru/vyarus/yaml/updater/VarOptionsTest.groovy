package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.report.UpdateReport

import java.nio.file.Files

/**
 * @author Vyacheslav Rusakov
 * @since 08.10.2021
 */
class VarOptionsTest extends AbstractTest {

    def "Check vars aggregation"() {

        setup: "prepare empty file"
        File file = Files.createTempFile('test', '.yml').toFile()

        when: "specifying multiple var maps"
        UpdateReport report = YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .vars(['foo': 'bar'])
                .vars(['baa': 'baz'])
                .update()

        then: "replaced"
        unifyString(report.dryRunResult) == """prop1:
  prop2: bar
  prop3: baz
  prop4: #{var}
"""

        cleanup:
        file.delete()
    }

    def "Check single var aggregation"() {

        setup: "prepare empty file"
        File file = Files.createTempFile('test', '.yml').toFile()

        when: "specifying multiple var maps"
        UpdateReport report = YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .vars(['foo': 'bar'])
                .var('baa', 'baz')
                .update()

        then: "replaced"
        unifyString(report.dryRunResult) == """prop1:
  prop2: bar
  prop3: baz
  prop4: #{var}
"""

        cleanup:
        file.delete()
    }

    def "Check vars file usage"() {

        setup: "prepare empty file"
        File file = Files.createTempFile('test', '.yml').toFile()

        when: "specifying multiple var maps"
        UpdateReport report = YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .varsFile('/prop/test.properties', true)
                .update()

        then: "replaced"
        unifyString(report.dryRunResult) == """prop1:
  prop2: bar
  prop3: baz
  prop4: #{var}
"""

        cleanup:
        file.delete()
    }

    def "Check vars not found cases"() {

        setup: "prepare empty file"
        File file = Files.createTempFile('test', '.yml').toFile()

        when: "specifying not existing file"
        UpdateReport report = YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .varsFile('bad.properties', false)
                .update()

        then: "missed vars ignored"
        unifyString(report.dryRunResult) == """prop1:
  prop2: #{foo}
  prop3: #{baa}
  prop4: #{var}
"""

        when: "missed file should not be ignored"
        YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .varsFile('bad.properties', true)
                .update()

        then: "error"
        thrown(IllegalArgumentException)

        cleanup:
        file.delete()
    }

    def "Check all vars aggregation"() {

        setup: "prepare empty file"
        File file = Files.createTempFile('test', '.yml').toFile()

        when: "specifying multiple var maps"
        UpdateReport report = YamlUpdater.createTest(file.absolutePath, '/common/vars.yml')
                .varsFile('/prop/test.properties', true)
                .vars(['var': '1'])
                .var('baa', '2')
                .update()

        then: "replaced"
        unifyString(report.dryRunResult) == """prop1:
  prop2: bar
  prop3: 2
  prop4: 1
"""

        cleanup:
        file.delete()
    }
}
