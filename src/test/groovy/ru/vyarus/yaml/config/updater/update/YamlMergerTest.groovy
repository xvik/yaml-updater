package ru.vyarus.yaml.config.updater.update


import ru.vyarus.yaml.config.updater.parse.comments.CommentsReader
import ru.vyarus.yaml.config.updater.parse.comments.CommentsWriter
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
class YamlMergerTest extends Specification {

    def "Check simple merge"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/simple.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/simple_upd.yml').toURI()))
        YamlMerger.merge(tree, upd)

        then: "merged"
        CommentsWriter.write(tree) == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2
  # comment line
  prop1.3: 1.3

# in the middle
prop11:
  prop11.1: 11.1

prop2:

  # sub comment
  prop2.1: 2.1

  list:
    - one
    - two

  obj:
    - one: 1
      two: 2
      three: 3

# comment changed
pppp: some

# complex

# comment
prop3:
  prop3.1: 3.1
"""

    }

    def "Check merge with shift"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/simple.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/simple_shifted_upd.yml').toURI()))
        YamlMerger.merge(tree, upd)

        then: "merged"
        CommentsWriter.write(tree) == """# something

# something 2
prop1:
    prop1.1: 1.1

    prop1.2: 1.2
    # comment line
    prop1.3: 1.3

# in the middle
prop11:
    prop11.1: 11.1

prop2:

    # sub comment
    prop2.1: 2.1

    list:
        - one
        - two
  
    obj:
        - one: 1
          two: 2
          three: 3

# comment changed
pppp: some

# complex

# comment
prop3:
    prop3.1: 3.1
"""

    }

    def "Check merge with negative shift"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/simple_shifted_upd.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/simple.yml').toURI()))
        YamlMerger.merge(tree, upd)

        then: "merged"
        CommentsWriter.write(tree) == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2
  # comment line
  prop1.3: 1.3

# in the middle
prop11:
    prop11.1: 11.1

prop2:

  # sub comment
  prop2.1: 2.1

  list:
    - one
    - two
    - three

  obj:
    - one: 22
      two: 22
      three: 3
    - one: 1
      two: 2
      three: 3

# original comment
pppp: some

# complex

# comment
prop3:
    prop3.1: 3.1
"""

    }

    def "Check multiline shift"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/multiline.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/multiline_upd.yml').toURI()))
        YamlMerger.merge(tree, upd)

        then: "merged"
        CommentsWriter.write(tree) == """object:
    simple: value with
      multiple lines (flow)

    include_newlines: |
      exactly as you see
      will appear these three
      lines of poetry

    middle_newlines: |
      exactly as you see
      will appear these three

      lines of poetry

    sub: |2
        first line
      second line
"""

    }


    def "Check multiline negative shift"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/multiline_upd.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/multiline.yml').toURI()))
        YamlMerger.merge(tree, upd)

        then: "merged"
        CommentsWriter.write(tree) == """object:
  simple: value with
      multiple lines (flow)

  include_newlines: |
      exactly as you see
      will appear these three
      lines of poetry

  middle_newlines: |
      exactly as you see
      will appear these three

      lines of poetry

  sub: |4
        first line
      second line
"""

    }

}
