package ru.vyarus.yaml.updater.parse.struct.model

import ru.vyarus.yaml.updater.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 19.08.2021
 */
class StructNodeTest extends AbstractTest {

    def "Check struct node to string"() {

        when: "property"
        StructNode node = StructNodeFactory.createProperty(null, 0, 1, "foo", '1')

        then:
        node.toString() == 'foo: 1'

        when: "subtree root"
        node = StructNodeFactory.createProperty(null, 0, 1, "foo", null)

        then:
        node.toString() == 'foo:'

        when: "list value"
        node = StructNodeFactory.createListValue(null, 0, 1, "foo")

        then:
        node.toString() == '- foo'

        when: "list item"
        node = StructNodeFactory.createListObject(null, 0, 1)

        then:
        node.toString() == '- '
    }
}
