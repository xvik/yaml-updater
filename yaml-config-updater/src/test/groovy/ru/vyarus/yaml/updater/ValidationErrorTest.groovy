package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.listen.UpdateListenerAdapter
import ru.vyarus.yaml.updater.parse.comments.model.CmtNodeFactory
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree
import spock.lang.TempDir

import java.util.function.Consumer

/**
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
class ValidationErrorTest extends AbstractTest {

    @TempDir
    File dir

    File current
    File update

    void setup() {
        current = new File(dir, "config.yml")
        current << """
one: 1
ff: 1
list:
    - one
    - two
"""
        update = new File(dir, "update.yml")
        update << """
one: 2
two: 2
list:
    - one
    - two
    - three

obj:
  - one: 1    
"""
    }

    def "Check changed old value detection"() {

        when: "original config's value not preserved"
        exec({it.find('one').value = [" 2"]})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Invalid value on path 'one': '2' when should remain from old file '1'"
    }

    def "Check changed new value detection"() {

        when: "update config's value changed"
        exec({it.find('two').value = [" 3"]})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Invalid value on path 'two': '3' when should be from update file '2'"
    }

    def "Check unknown value appears"() {

        when: "add additional node"
        exec({it.children.add(CmtNodeFactory.createProperty(null, 0, 3, 'other', ' 12'))})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Property 'other' not found neither in old nor in new file: '12'"
    }

    def "Check unknown list item appears"() {

        when: "add additional node"
        exec({it.find('obj').children.add(CmtNodeFactory.createListObject(null, 2, 10,
            CmtNodeFactory.createProperty(null, 4, 11, 'foo', ' bar')))})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Can't find reference list item neither in old nor in new file: obj[1]"
    }

    def "Check current value disappear"() {

        when: "remove old config's value"
        exec({it.getChildren().remove(it.find('ff'))})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Value 'ff' disappeared (should remain from original file): '1'"
    }

    def "Check new value disappear"() {

        when: "remove new config's value"
        exec({it.getChildren().remove(it.find('two'))})

        then: "detected"
        def ex = thrown(IllegalStateException)
        ex.cause.cause.message == "Value 'two' from update file was not added: '2'"
    }

    private void exec(Consumer<CmtTree> callback) {
        UpdLst list = new UpdLst(callback)
        YamlUpdater.create(current, update).backup(false).listen(list).update()
    }

    static class UpdLst extends UpdateListenerAdapter {

        Consumer<CmtTree> fun

        UpdLst(Consumer<CmtTree> fun) {
            this.fun = fun
        }

        @Override
        void merged(CmtTree result) {
            fun.accept(result)
        }
    }
}
