package ru.vyarus.yaml.updater.parse.comments

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree

/**
 * @author Vyacheslav Rusakov
 * @since 03.05.2021
 */
class WriterTest extends AbstractTest {

    def "Check read-write"() {

        setup: "original file"
        println "processing $file"
        String original = new File(getClass().getResource("/common/$file").toURI()).text

        when: "reading and writing"
        CmtTree tree = CommentsReader.read(original)
        String result = CommentsWriter.write(tree)

        then: "not changed"
        unifyString(result) == unifyString(original)

        where:
        file            | _
        'sample.yml'    | _
        'multiline.yml' | _
        'lists.yml'     | _
        'complex.yml'   | _
        'sequences.yml' | _
        'quotes.yml'    | _
    }
}
