package ru.vyarus.yaml.updater.update

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.parse.comments.CommentsReader
import ru.vyarus.yaml.updater.parse.comments.model.CmtNode
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree

/**
 * @author Vyacheslav Rusakov
 * @since 23.08.2021
 */
class ListMatchTest extends AbstractTest {

    def "Check list item match"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      two: 3
    - one: 1
      two: 2  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "selected second item"
        selected != null
        selected.find('two').getIdentityValue() == '2'
    }

    def "Check different notions match"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - 
      one: 1
      two: 3
    - 
      one: 1
      two: 2  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "selected second item"
        selected != null
        selected.find('two').getIdentityValue() == '2'
    }

    def "Check list item match by subtree"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      sub:
        two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      sub:
        two: 3
    - one: 1
      sub:
        two: 2  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "selected second item"
        selected != null
        selected.find('sub/two').getIdentityValue() == '2'
    }

    def "Check list item match 2"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - a: 3
      b: 1
""")
        CmtTree two = CommentsReader.read("""
list:
    - a: 3
      b: 1
    - d: 3
      b: 1  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "selected first item"
        selected != null
        selected.find('a').getIdentityValue() == '3'
    }


    def "Check more then one match"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      two: 2
      three: 3
    - one: 1
      two: 2
      three: 4  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "nothing selected (no unique match)"
        selected == null
    }

    def "Check incomplete match"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      sub:
        two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      sub:
        two: 3
    - one: 1  
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "selected second item, because only it produce exact match"
        selected != null
        selected.getChildren().size() == 1
    }

    def "Check property without value"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      two: 3
    - one: 1
      two:   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "nothing matched (no value - can't be used for matching)"
        selected == null
    }


    def "Check property without value 2"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two:
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      two: 3
    - one: 1
      two: 2   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "both items matches - nothing selected"
        selected == null
    }

    def "Check no intercection"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
""")
        CmtTree two = CommentsReader.read("""
list:
    - a: 1
      b: 3
    - a: 1
      b: 2   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "nothing matched"
        selected == null
    }

    def "Check match with comment"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      two: 2
      # cmt
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      two: 3
    - one: 1
      two: 2   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "second matches"
        selected != null
        selected.find('two').getIdentityValue() == '2'
    }

    def "Check match with sublist"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      sub: 
        - a
        - b
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 2
      sub:
        - b
        - c  
    - one: 1
      sub:
        - c
        - d   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "second matches ignoring sublist"
        selected != null
        selected.find('sub[0]').getIdentityValue() == 'c'
    }

    def "Check match with deep sublist"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      sub: 
        - a: 3
          b: 1
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      sub:
        - a: 3
          b: 2
        - c: 3
          b: 3  
    - one: 1
      sub:
        - a: 3
          b: 1
        - d: 3
          b: 1   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "second matches"
        selected != null
        selected.find('sub[1]/d') != null
    }

    def "Check match with deep sublist 2"() {

        setup:
        CmtTree one = CommentsReader.read("""
list:
    - one: 1
      sub: 
        - a:
            b: 1
""")
        CmtTree two = CommentsReader.read("""
list:
    - one: 1
      sub:
        - a:
            b: 2
        - c:
            b: 3  
    - one: 1
      sub:
        - a:
            b: 1
        - d:
            b: 1   
""")
        def item = one.find('list[0]')
        def items = two.find('list').getChildren()

        when: "matching item"
        CmtNode selected = ListMatcher.match(item, items)
        then: "second matches"
        selected != null
        selected.find('sub[1]/d/b') != null
    }
}
