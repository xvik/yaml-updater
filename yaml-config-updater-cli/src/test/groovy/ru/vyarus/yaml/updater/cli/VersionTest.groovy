package ru.vyarus.yaml.updater.cli

/**
 * @author Vyacheslav Rusakov
 * @since 27.08.2021
 */
class VersionTest extends AbstractTest {

    def "Check version call"() {

        expect: "executing version command"
        // skipping version because different test runs may bring or not jacoco meta-inf
        runWithOutput("-V").startsWith("yaml-config-updater version ")

    }
}
