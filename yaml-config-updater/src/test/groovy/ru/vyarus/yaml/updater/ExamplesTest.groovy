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
        and: "report correct"
        lastReport == """Configuration: /tmp/CONFIG.yml (300 bytes, 23 lines)
Updated from source of 497 bytes, 25 lines
Resulted in 301 bytes, 25 lines

\tAdded from new file:
\t\tprop/three                               7  | three: 3                              # new property
\t\tlists/obj[0]/three                       20 | three: 3                        # new value
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

    def "Check object list style changed"() {
        expect:
        merge("""
list: 
  - one: 1
    two: 2
""",
                """
list: 
  - 
    one: 1
    two: 2
""") == """
list: 
  -
    one: 1
    two: 2
"""
    }

    private String lastReport

    private String merge(String source, String update) {
        File current = new File(dir, "config.yml")
        current << source.trim()
        long curSize = current.length()
        File upd = new File(dir, "update.yml")
        upd << update.trim()

        def report = YamlUpdater.create(current, upd).backup(false).update()
        lastReport = print(report, curSize, current.size())
                .replace(current.getAbsolutePath(), '/tmp/CONFIG.yml')

        unifyString("\n" + current.text)
    }
}
