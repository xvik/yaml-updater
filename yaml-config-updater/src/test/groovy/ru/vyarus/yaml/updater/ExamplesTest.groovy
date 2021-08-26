package ru.vyarus.yaml.updater

import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 25.08.2021
 */
class ExamplesTest extends AbstractTest {

    @TempDir
    File dir

    def "Check example"() {

        expect:
        merge("""
# top comment

# something
prop:
  one: 1

  two: 2

lists:

  # sub comment
  list:
    - one
    - two

  obj:
    - one: 1
      two: 2

large: multi-line
  value

# trailing comment
""",
                """
# changed comment

# something else
prop:
  one: 3   
  two: 3
  three: 3                              # new property

lists:

  # changed sub comment
  list:                                 # padding changed
      - two                             # order changed (ignored)
      - one      
      - three                           # new value ignored

  obj:
      - two: 2
        one: 1
        three: 3                        # new value

large: multi-line
  value

# changed trailing comment
""") == """
# changed comment

# something else
prop:
  one: 1

  two: 2
  three: 3                              # new property

lists:

  # changed sub comment
  list:
      - one
      - two

  obj:
      - two: 2
        one: 1
        three: 3                        # new value

large: multi-line
  value

# changed trailing comment
"""
    }

    def "Check new property added"() {
        expect:
        merge("""
one: 1
two: 2
""",
                """
one: 3
two: 3
three: 3
""") == """
one: 1
two: 2
three: 3
"""
    }

    def "Check order changed"() {
        expect:
        merge("""
one: 1
two: 2
""",
                """
three: 3
two: 3
one: 3
""") == """
three: 3
two: 2
one: 1
"""
    }

    def "Check padding changed"() {
        expect:
        merge("""
one: 
  two: 2
""",
                """
one: 
    two: 3
""") == """
one: 
    two: 2
"""
    }

    def "Check comment updated"() {
        expect:
        merge("""
one: 
  # Old comment
  two: 2
""",
                """
one: 
    # New comment
    two: 3
""") == """
one: 
    # New comment
    two: 2
"""
    }

    def "Check scalar list not updated"() {
        expect:
        merge("""
list: 
  - one
  - two
""",
                """
list: 
    - one
    - three
""") == """
list: 
    - one
    - two
"""
    }

    def "Check object list item updated"() {
        expect:
        merge("""
list: 
  - one: 1
    two: 2
""",
                """
list: 
  - one: 1
    two: 2
    three: 3
""") == """
list: 
  - one: 1
    two: 2
    three: 3
"""
    }

    private String merge(String source, String update) {
        File current = new File(dir, "config.yml")
        current << source.trim()
        File upd = new File(dir, "update.yml")
        upd << update.trim()

        YamlUpdater.create(current, upd).backup(false).update()

        unifyString("\n" + current.text)
    }
}
