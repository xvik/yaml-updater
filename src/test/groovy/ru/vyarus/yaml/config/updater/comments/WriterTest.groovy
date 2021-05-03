package ru.vyarus.yaml.config.updater.comments

import ru.vyarus.yaml.config.updater.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 03.05.2021
 */
class WriterTest extends Specification {

    def "Check read-write"() {

        setup: "original file"
        println "processing $file"
        String original = new File(getClass().getResource("/comments/$file").toURI()).text

        when: "reading and writing"
        YamlTree tree = CommentsReader.read(original)
        String result = CommentsWriter.write(tree)

        then: "not changed"
        original == result

        where:
        file            | _
        'sample.yml'    | _
        'multiline.yml' | _
        'lists.yml'     | _
    }
}
