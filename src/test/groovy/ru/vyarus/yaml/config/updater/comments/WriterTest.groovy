package ru.vyarus.yaml.config.updater.comments

import ru.vyarus.yaml.config.updater.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 03.05.2021
 */
class WriterTest extends Specification {

    def "Check dummy read-write"() {

        setup: "original file"
        String original = new File(getClass().getResource('/comments/sample.yml').toURI()).text

        when: "reading and writing"
        YamlTree tree = CommentsReader.read(original)
        String result = CommentsWriter.write(tree)

        then: "not changed"
        original == result
    }
}
