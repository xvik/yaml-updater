package ru.vyarus.yaml.config.updater.parse.comments

import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 26.04.2021
 */
class ParseTest extends Specification {

    def "Check simple case"() {

        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 4
    }

    def "Check multiline values"() {
        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/common/multiline.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 12
    }

    def "Check list values"() {
        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 5
    }
}
