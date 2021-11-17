package ru.vyarus.yaml.updater.parse.comments.model

import ru.vyarus.yaml.updater.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
class CmtNodeTest extends AbstractTest {

    def "Check comment node to string"() {

        when: "property"
        CmtNode node = CmtNodeFactory.createProperty(null, 0, 1, "foo", ' 1')

        then:
        node.toString() == 'foo: 1'

        when: "subtree root"
        node = CmtNodeFactory.createProperty(null, 0, 1, "foo", null)

        then:
        node.toString() == 'foo:'

        when: "list value"
        node = CmtNodeFactory.createListValue(null, 0, 1, "foo")

        then:
        node.toString() == '- foo'

        when: "list item"
        node = CmtNodeFactory.createListObject(null, 0, 1)

        then:
        node.toString() == '- '

        when: "comment only"
        node = CmtNodeFactory.createProperty(null, 0, 1, null)
        node.getTopComment().addAll(['sample', 'comment'])

        then:
        node.toString() == 'sample'
    }

    def "Check identity value"() {

        when: "simple value"
        CmtNode node = CmtNodeFactory.createProperty(null, 0, 1, "foo", ' 1')

        then:
        node.getIdentityValue() == '1'

        when: "multiline value"
        node = CmtNodeFactory.createProperty(null, 0, 1, "foo", ' 1', '2', '3')

        then:
        node.getIdentityValue() == '123'

        when: "with comments value"
        node = CmtNodeFactory.createProperty(null, 0, 1, "foo", null, ' 1 #hmm', '      2 #sometyhing', '3')

        then:
        node.getIdentityValue() == '123'
    }

    def "Check source key update"() {
        setup:
        CmtNode node = CmtNodeFactory.createProperty(null, 0, 1, "foo", ' 1')

        when: "updating source key"
        node.setSourceKey("'foo'")
        then: "ok"
        node.getKey() == 'foo'
        node.getSourceKey() == "'foo'"

        when: "invalid update"
        node.setSourceKey("'bar'")
        then: "error"
        thrown(IllegalStateException)
    }
}
