package ru.vyarus.yaml.updater.parse.struct


import ru.vyarus.yaml.updater.parse.struct.model.StructNode
import ru.vyarus.yaml.updater.parse.struct.model.StructTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 29.05.2021
 */
class TreePathsTest extends Specification {
    def "Check simple parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/sample.yml').toURI()))

        then: "generating leaf paths"
        toPaths(tree) == """   5| prop1/prop1.1
   7| prop1/prop1.2
  12| prop2/prop2.1
  17| prop3"""
    }

    def "Check lists parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/lists.yml').toURI()))

        then: "generating leaf paths"
        toPaths(tree) == """   2| simple_list[0]
   5| simple_list[1]
   7| simple_list[2]
   8| simple_list[3]
   9| simple_list[4]
  13| object[0]/one
  14| object[0]/two
  16| object[1]/one
  17| object[1]/two
  22| object2[0]/one
  23| object2[0]/two
  26| object2[1]/one
  27| object2[1]/two
  31| object3[0]/one
  33| object3[0]/two/three
  34| object3[0]/two/four
  36| object3[0]/and[0]
  37| object3[0]/and[1]
  42| map_of_maps/one/a1
  43| map_of_maps/one/a2
  45| map_of_maps/two/b1
  46| map_of_maps/two/b2
  50| sublist[0]/one/sub1
  51| sublist[0]/two
  55| emptyLine[0]"""
    }

    def "Check multiline parse"() {

        when: "parsing"
        StructTree tree = StructureReader.read(new File(getClass().getResource('/common/multiline.yml').toURI()))

        then: "generating leaf paths"
        toPaths(tree) == """   1| simple
   4| quoted
   7| quoted2
  10| multiline
  14| multiline2
  19| include_newlines
  25| middle_newlines
  33| fold_newlines
  39| ignore_ind
  44| append_ind
  49| custom_indent
  56| custom_indent2
  62| object/sub
  67| list[0]
  69| list[1]/obj
  71| list[2]/ob2
  75| flow"""
    }

    private String toPaths(StructTree tree) {
        return toPaths(tree.getTreeLeaves()).join('\n')
    }

    private List<String> toPaths(List<StructNode> props) {
        List<String> res = []
        for(StructNode prop: props) {
            if (prop.hasListValue()) {
                // processing list items
                prop.getChildren().each { res.addAll(toPaths(it.getAllPropertiesIncludingScalarLists())) }
            } else {
                res.add(String.format("%4s| ", prop.getLineNum()) + prop.getYamlPath());
            }
        }
        return res;
    }
}
