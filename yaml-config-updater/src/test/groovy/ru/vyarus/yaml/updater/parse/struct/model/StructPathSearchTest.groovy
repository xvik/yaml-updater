package ru.vyarus.yaml.updater.parse.struct.model

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.struct.StructureReader

/**
 * @author Vyacheslav Rusakov
 * @since 21.08.2021
 */
class StructPathSearchTest extends AbstractTest {

    def "Check simple search"() {

        when: "parsing file"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))

        then: "searching"
        tree.find('prop1').key == 'prop1'
        tree.find('prop1/prop1.1').key == 'prop1.1'
        tree.find('prop1/prop1.2').key == 'prop1.2'
        tree.find('prop3').key == 'prop3'
        tree.find('prop10') == null
    }

    def "Check lists search"() {

        when: "parsing file"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))

        then: "simple case"
        tree.find('simple_list[0]').value == 'one'
        tree.find('simple_list[1]').value == 'two'

        and: "empty dash case"
        tree.find('object2[0]/one').key == 'one'

        and: "third level case"
        tree.find('object3[0]/two').key == 'two'
        tree.find('object3[0]/two/three').value == '3'
        tree.find('object3[0]/and').key == 'and'
        tree.find('object3[0]/and[0]').value == 'sub1'
    }
}
