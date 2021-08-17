package ru.vyarus.yaml.updater

import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 17.08.2021
 */
class ErrorsTest extends AbstractTest {

    @TempDir
    File dir

    def "Check invalid current file"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        current << "invalid content"
        File update = new File(dir, "update.yml")
        update << "some:"


        when: "updating"
        YamlUpdater.create(current, update).backup(false).update()

        then: "error"
        def ex = thrown(IllegalStateException)
        ex.getCause().message.startsWith('Failed to parse yaml file')
    }

    def "Check no update file"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        current << "some:"
        File update = new File(dir, "update.yml")


        when: "updating"
        YamlUpdater.create(current, update).backup(false).update()

        then: "error"
        def ex = thrown(IllegalStateException)
        ex.message.startsWith('Error updating from file')
    }


    def "Check empty update file"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        current << "some:"
        File update = new File(dir, "update.yml")
        update.createNewFile()

        when: "updating"
        YamlUpdater.create(current, update).backup(false).update()

        then: "error"
        def ex = thrown(IllegalStateException)
        ex.message.startsWith('Error updating from file')
    }

    def "Check invalid update file"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        current << "some:"
        File update = new File(dir, "update.yml")
        update << "invalid"

        when: "updating"
        YamlUpdater.create(current, update).backup(false).update()

        then: "error"
        def ex = thrown(IllegalStateException)
        ex.message.startsWith('Failed to update: original configuration remains')
    }

    def "Check incorrect configuration"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        current << "some:"
        File update = new File(dir, "update.yml")
        update << "other:"

        when: "updating"
        YamlUpdater.create(null, update).backup(false).update()

        then: "error"
        def ex = thrown(IllegalStateException)
        ex.message.startsWith('Error updating from file')

        when: "updating"
        YamlUpdater.create(current, null as InputStream).backup(false).update()

        then: "error"
        def ex2 = thrown(IllegalArgumentException)
        ex2.message.startsWith('New config file not specified')

        when: "updating"
        YamlUpdater.create(current, null as File).backup(false).update()

        then: "error"
        def ex3 = thrown(IllegalStateException)
        ex3.message.startsWith('Error updating from file')
    }
}
