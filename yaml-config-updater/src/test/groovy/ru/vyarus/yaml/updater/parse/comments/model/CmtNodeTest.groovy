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

    }
}
