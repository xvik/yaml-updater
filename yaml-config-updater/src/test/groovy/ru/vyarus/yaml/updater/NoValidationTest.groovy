package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.listen.UpdateListenerAdapter
import ru.vyarus.yaml.updater.parse.struct.model.StructTree
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
class NoValidationTest extends AbstractTest {

    @TempDir
    File dir

    def "Check validation called"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = new File(dir, "update.yml")
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "updating"
        UpdLst list = new UpdLst()
        YamlUpdater.create(current, update).backup(false).listen(list).update()

        then: "validation performed"
        list.validated
    }

    def "Check validation disabled"() {

        setup: "prepare files"
        File current = new File(dir, "config.yml")
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = new File(dir, "update.yml")
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "updating"
        UpdLst list = new UpdLst()
        YamlUpdater.create(current, update).backup(false).validateResult(false).listen(list).update()

        then: "validation not performed"
        !list.validated
    }

    static class UpdLst extends UpdateListenerAdapter {
        boolean validated

        @Override
        void validated(StructTree result) {
            this.validated = true
        }
    }

}
