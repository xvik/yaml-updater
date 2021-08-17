package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.listen.UpdateListener
import ru.vyarus.yaml.updater.listen.UpdateListenerAdapter
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
        UpdLst list = new UpdLst()
        YamlUpdater.create(current, update).backup(true).listen(list).update()

        then: "backup created"
        dir.list().size() == 3
        list.backup != null
    }

    static class UpdLst extends UpdateListenerAdapter {
        File backup;

        @Override
        void backupCreated(File backup) {
            this.backup = backup
        }
    }


    def "Check no backup creation"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = new File(dir, "update.yml")
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)
        UpdLst list = new UpdLst()

        when: "updating"
        YamlUpdater.create(current, update).backup(false).listen(list).update()

        then: "backup created"
        dir.list().size() == 2
        list.backup == null
    }
}
