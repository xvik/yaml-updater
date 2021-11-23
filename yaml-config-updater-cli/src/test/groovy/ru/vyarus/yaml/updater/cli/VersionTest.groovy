package ru.vyarus.yaml.updater.cli

/**
 * @author Vyacheslav Rusakov
 * @since 27.08.2021
 */
class VersionTest extends AbstractTest {

    def "Check version call"() {
        // sample version file is in test resources META-INF/VERSION

        when: "executing version command"
        def out = runWithOutput("-V")

        then: "version not found"
        out.startsWith("1.0 (10 NOV 2021)")

    }
}
