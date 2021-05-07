package ru.vyarus.yaml.config.updater.comments

import ru.vyarus.yaml.config.updater.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 26.04.2021
 */
class ParseTest extends Specification {

    def "Check simple case"() {

        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/comments/sample.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 4
    }

    def "Check multiline values"() {
        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/comments/multiline.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 12
    }

    def "Check list values"() {
        when: "parsing file"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/comments/lists.yml').toURI()))
        println tree

        then: "parsed"
        tree.nodes.size() == 5
    }
}
