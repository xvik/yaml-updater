package ru.vyarus.yaml.updater.parse.comments

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree
import ru.vyarus.yaml.updater.parse.struct.StructureReader
import ru.vyarus.yaml.updater.parse.struct.model.StructTree
import ru.vyarus.yaml.updater.update.CommentsParserValidator

/**
 * @author Vyacheslav Rusakov
 * @since 18.08.2021
 */
class ParserValidatorErrorTest extends AbstractTest {

    def "Check success validation"() {

        setup:
        def file = """
one: 1
#cmt
two: 2

list:
    - one
    - two:
        sub: 1
        sub: 2
        
multiline: value with
    multiline        
"""

        expect: "models equal"
        compare(file, file)
    }

    def "Check success validation with comments"() {

        setup:
        def file = """
one: 1
#two: 2        
"""

        expect: "models equal"
        compare(file, file)
    }

    def "Check different children"() {

        when: "models with different children"
        compare("""
one: 
    sub: s
""", """
one: 
    sub: s
two: 2
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 0: 1 child nodes found but should be at least 2 (this is a parser bug, please report it!)
      Comments parser subtree:    Structure parser subtree:
         2| one:                     2| one:
         3|   sub: s                 3|   sub: s
                                     4| two: 2
"""
    }

    def "Check too much children"() {

        when: "models with different children"
        compare("""
one: 1
two: 2
""", """
one: 1
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 3: line should not exist (this is a parser bug, please report it!)
         3| two: 2
"""
    }

    def "Check different nodes"() {

        when: "models with different children"
        compare("""
one: 1
three: 3
""", """
one: 1
two: 2
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 3: line should be different: "two: 2" (this is a parser bug, please report it!)
      Comments parser subtree:    Structure parser subtree:
         2| one: 1                   2| one: 1
         3| three: 3                 3| two: 2
"""
    }

    def "Check object lists render"() {

        when: "models with different list name"
        compare("""
one:
    - prop1: val
      prop2: val
""", """
two:
    - pp1: v
      pp2: v
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 2: line should be different: "two:" (this is a parser bug, please report it!)
      Comments parser subtree:    Structure parser subtree:
         2| one:                     2| two:
         3|   - prop1: val           3|   - pp1: v
         4|     prop2: val           4|     pp2: v
"""
    }

    def "Check object lists render 2"() {

        when: "models with different list name"
        compare("""
one:
    - 
      prop1: val
      prop2: val
""", """
two:
    - 
      pp1: v
      pp2: v
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 2: line should be different: "two:" (this is a parser bug, please report it!)
      Comments parser subtree:    Structure parser subtree:
         2| one:                     2| two:
         3|   -                      3|   - 
         4|     prop1: val           4|     pp1: v
         5|     prop2: val           5|     pp2: v
"""
    }

    def "Check scalar lists render"() {

        when: "models with different list name"
        compare("""
one:
    - val
    - val2
""", """
two:
    - pp
    - pp2
""")

        then: "error detected"
        def ex = thrown(IllegalStateException)
        ex.message == """Comments parser validation problem on line 2: line should be different: "two:" (this is a parser bug, please report it!)
      Comments parser subtree:    Structure parser subtree:
         2| one:                     2| two:
         3|   -  val                 3|   - pp
         4|   -  val2                4|   - pp2
"""
    }

    private boolean compare(String cmtSrc, structSrc) {
        CmtTree cmt = CommentsReader.read(cmtSrc)
        StructTree struct = StructureReader.read(structSrc)
        CommentsParserValidator.validate(cmt, struct)
        return true // otherwise throw error
    }
}
