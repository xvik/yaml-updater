package ru.vyarus.yaml.config.updater.parse.struct


import ru.vyarus.yaml.config.updater.parse.struct.model.StructTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
class StructParseTest extends Specification {

    def "Check simple parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))
        
        then: "ok"
        tree.children.size() == 3
        tree.toString() == """prop1: 
  prop1.1: '1.1'
  prop1.2: '1.2'
prop2: 
  prop2.1: '2.1'
prop3: '3'
"""
    }

    def "Check lists parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))

        then: "ok"
        tree.children.size() == 6
        tree.toString() == """simple_list: 
  - 'one'
  - 'two'
  - 'three'
  - 'prop: like'
  - 'multiline property'
object: 
  - one: '1'
    two: '2'
  - one: '1.1'
    two: '2.2'
object2: 
  - 
    one: '1'
    two: '2'
  - 
    one: '1.1'
    two: '2.2'
object3: 
  - one: '1'
    two: 
      three: '3'
      four: '4'
    and: 
      - 'sub1'
      - 'sub2'
map_of_maps: 
  one: 
    a1: '1'
    a2: '2'
  two: 
    b1: '1'
    b2: '2'
sublist: 
  - one: 
      sub1: '1'
    two: '2'
"""
    }

    def "Check multiline parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/multiline.yml').toURI()))

        then: "ok"
        tree.children.size() == 13
        tree.toString() == """simple: 'value with multiple lines (flow)'
quoted: 'value with multiple lines'
quoted2: 'value with multiple lines'
include_newlines: 'exactly as you see\\nwill appear these three\\nlines of poetry\\n'
middle_newlines: 'exactly as you see\\nwill appear these three\\n\\nlines of poetry\\n'
fold_newlines: 'this is really a single line of text\\ndespite appearances\\n'
ignore_ind: 'this is really a single line of text despite appearances'
append_ind: 'this is really a single line of text despite appearances\\n\\n'
custom_indent: '  this is really a\\nsingle line of text despite appearances\\n'
custom_indent2: '  this is really a\\nsingle line of text despite appearances'
object: 
  sub: '  first line\\nsecond line\\n'
list: 
  - 'something more then one line'
  - obj: 'with multiline value'
  - ob2: 'multiline\\nmarker\\n'
flow: 'something before whitespace\\nsomething after whitespace'
"""
    }
}
