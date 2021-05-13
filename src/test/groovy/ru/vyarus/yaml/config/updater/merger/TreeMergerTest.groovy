package ru.vyarus.yaml.config.updater.merger


import ru.vyarus.yaml.config.updater.parse.comments.CommentsReader
import ru.vyarus.yaml.config.updater.parse.comments.CommentsWriter
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
class TreeMergerTest extends Specification {

    def "Check simple merge"() {

        when: "merging"
        YamlTree tree = CommentsReader.read(new File(getClass().getResource('/merge/simple.yml').toURI()))
        YamlTree upd = CommentsReader.read(new File(getClass().getResource('/merge/simple_upd.yml').toURI()))
        TreeMerger.merge(tree, upd)

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
}
