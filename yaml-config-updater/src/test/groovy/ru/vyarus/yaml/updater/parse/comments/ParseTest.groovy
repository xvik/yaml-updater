package ru.vyarus.yaml.updater.parse.comments


import ru.vyarus.yaml.updater.parse.comments.model.CmtTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 26.04.2021
 */
class ParseTest extends Specification {

    def "Check simple case"() {

        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))

        then: "parsed"
        tree.children.size() == 4
        tree.toString() == """# comment 3 lines
prop1: ''
  prop1.1: '1.1'
  # comment
  prop1.2: '1.2'
# comment
prop2: ''
  # comment 2 lines
  prop2.1: '2.1'
# comment 4 lines
prop3: '3'
# comment 5 lines
"""
    }

    def "Check multiline values"() {
        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/multiline.yml').toURI()))

        then: "parsed"
        tree.children.size() == 15
        tree.toString() == """simple: value 2 lines
# comment
quoted: value 2 lines
# comment
quoted2: value 2 lines
# comment
multiline: value 3 lines
# comment
multiline2: value 4 lines
# comment
include_newlines: value 5 lines
# comment
middle_newlines: value 5 lines
# comment 3 lines
fold_newlines: value 5 lines
# comment
ignore_ind: value 4 lines
# comment
append_ind: value 5 lines
custom_indent: value 4 lines
# comment 3 lines
custom_indent2: value 4 lines
# comment
object: ''
  sub: value 3 lines
# comment
list: ''
  - value 2 lines
  - obj: value 2 lines
  - ob2: value 3 lines
# comment
flow: value 3 lines
"""
    }

    def "Check list values"() {
        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))

        then: "parsed"
        tree.children.size() == 6
        // NOTE double quotes are ok below because value was in quotes and outer quotes are "technical"
        tree.toString() == """simple_list: ''
  - 'one'
  # comment 2 lines
  - 'two'
  # comment
  - 'three'
  - ''prop: like''
  - value 2 lines
# comment
object: ''
  - one: '1'
    two: '2'
  # comment
  - one: '1.1'
    two: '2.2'
# comment 2 lines
object2: ''
  - ''
    one: '1'
    two: '2'
  # comment
  - ''
    one: '1.1'
    two: '2.2'
# comment 2 lines
object3: ''
  - one: '1'
    two: ''
      three: '3'
      four: '4'
    and: ''
      - 'sub1'
      - 'sub2'
# comment 2 lines
map_of_maps: ''
  one: ''
    a1: '1'
    a2: '2'
  two: ''
    b1: '1'
    b2: '2'
# comment
sublist: ''
  - one: ''
      sub1: '1'
    two: '2'
"""
    }

    def "Check parse error"() {
        when: "parsing file"
        CommentsReader.read("""
one: 1
impossible
two: 2    
""")

        then: "parse error"
        def ex = thrown(IllegalStateException)
        ex.message == "Failed to read yaml string"
    }

    def "Check parse error 2"() {
        when: "parsing file"
        CommentsReader.read("""
one:
    two: 2 
    impossible    
""")

        then: "parse error"
        def ex = thrown(IllegalStateException)
        ex.message == "Failed to read yaml string"
    }


    def "Check sequence values"() {
        when: "parsing file"
        CmtTree tree = CommentsReader.read(new File(getClass().getResource('/common/sequences.yml').toURI()))

        then: "parsed"
        tree.children.size() == 8
        // NOTE double quotes are ok below because value was in quotes and outer quotes are "technical"
        tree.toString() == """line: '[1, 2, 4]'
# comment
multiLine: value 2 lines
# comment
empty: '[]'
# comment
object: '{ one: 1, two: 2 }'
# comment
multiObject: value 2 lines
# comment
emptyObj: '{}'
# comment
listOfArr: ''
    - '[1, 2]'
    - '[]'
# comment
listOfObj: ''
    - '{ one: 1, two: 3 }'
    - '{}'
"""
    }
}
