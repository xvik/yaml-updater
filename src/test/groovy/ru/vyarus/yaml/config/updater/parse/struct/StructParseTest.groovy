package ru.vyarus.yaml.config.updater.parse.struct


import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
class StructParseTest extends Specification {

    def "Check simple parse"() {

        when: "parsing"
        YamlStructTree tree = StructureReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))
        println tree
        
        then: "ok"
        tree.nodes.size() == 3
    }

    def "Check lists parse"() {

        when: "parsing"
        YamlStructTree tree = StructureReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))
        println tree

        then: "ok"
        tree.nodes.size() == 5
    }

    def "Check multiline parse"() {

        when: "parsing"
        YamlStructTree tree = StructureReader.read(new File(getClass().getResource('/common/multiline.yml').toURI()))
        println tree

        then: "ok"
        tree.nodes.size() == 13
    }
}
