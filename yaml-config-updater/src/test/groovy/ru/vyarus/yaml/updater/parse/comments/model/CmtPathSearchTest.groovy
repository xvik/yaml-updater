package ru.vyarus.yaml.updater.parse.comments.model

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.comments.CommentsReader

/**
 * @author Vyacheslav Rusakov
 * @since 21.08.2021
 */
class CmtPathSearchTest extends AbstractTest {

    def "Check simple search"() {

        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))

        then: "searching"
        tree.find('prop1').key == 'prop1'
        tree.find('prop1/prop1.1').key == 'prop1.1'
        tree.find('prop1/prop1.2').key == 'prop1.2'
        tree.find('prop3').key == 'prop3'
        tree.find('prop10') == null
    }

    def "Check lists search"() {

        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))

        then: "simple case"
        tree.find('simple_list[0]').value[0] == ' one'
        tree.find('simple_list[1]').value[0] == ' two'

        and: "empty dash case"
        tree.find('object2[0]/one').key == 'one'

        and: "third level case"
        tree.find('object3[0]/two').key == 'two'
        tree.find('object3[0]/two/three').value[0] == ' 3'
        tree.find('object3[0]/and').key == 'and'
        tree.find('object3[0]/and[0]').value[0] == ' sub1'
    }
}
