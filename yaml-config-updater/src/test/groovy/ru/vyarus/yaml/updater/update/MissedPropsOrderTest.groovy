package ru.vyarus.yaml.updater.update

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.comments.CommentsReader
import ru.vyarus.yaml.updater.parse.comments.CommentsWriter
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree

/**
 * @author Vyacheslav Rusakov
 * @since 25.07.2021
 */
class MissedPropsOrderTest extends AbstractTest {

    def "Check simple merge"() {

        when: "merging"
        CmtTree tree = CommentsReader.read("""
sub:
    prop1.1: 1.1
    
    prop1.2: 1.2
""")
        CmtTree upd = CommentsReader.read("""
sub:
    prop2: 12
""")
        TreeMerger.merge(tree, upd)

        then: "merged"
        unifyString(CommentsWriter.write(tree)) == """
sub:
    prop1.1: 1.1
    
    prop1.2: 1.2
    prop2: 12
"""
    }
}
