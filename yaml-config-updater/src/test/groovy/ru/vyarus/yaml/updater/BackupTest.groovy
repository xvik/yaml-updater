package ru.vyarus.yaml.updater

import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 17.08.2021
 */
class BackupTest extends AbstractTest {

    @TempDir
    File dir

    def "Check backup creation"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = new File(dir, "update.yml")
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "updating"
        YamlUpdater.create(current, update).backup(true).update()

        then: "backup created"
        dir.list().size() == 3
    }

    def "Check no backup creation"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = new File(dir, "update.yml")
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "updating"
        YamlUpdater.create(current, update).backup(false).update()

        then: "backup created"
        dir.list().size() == 2
    }
}
