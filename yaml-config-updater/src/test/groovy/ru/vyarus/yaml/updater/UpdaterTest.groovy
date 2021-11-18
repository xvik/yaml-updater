package ru.vyarus.yaml.updater

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2021
 */
class UpdaterTest extends AbstractTest {

    def "Check simple merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# something

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
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 23 lines)
Updated from source of 300 bytes, 40 lines
Resulted in 300 bytes, 36 lines

\tAdded from new file:
\t\tprop1/prop1.3                            9  | prop1.3: 1.3
\t\tprop11                                   12 | prop11:
\t\tprop2/obj[0]/three                       31 | three: 3
\t\tprop3                                    39 | prop3:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check shifted merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_shifted_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# something

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
        and: "report correct"
        print(report)  == """Configuration: /tmp/CONFIG.yml (300 bytes, 23 lines)
Updated from source of 300 bytes, 40 lines
Resulted in 300 bytes, 36 lines

\tAdded from new file:
\t\tprop1/prop1.3                            9  | prop1.3: 1.3
\t\tprop11                                   12 | prop11:
\t\tprop2/obj[0]/three                       31 | three: 3
\t\tprop3                                    39 | prop3:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check negative shifted merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_shifted_upd.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# something

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
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 40 lines)
Updated from source of 300 bytes, 23 lines
Resulted in 300 bytes, 40 lines

\tOnly comments, order or formatting changed
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check multiline values merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/multiline.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/multiline_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """object:
    simple: value with
      multiple lines (flow)

    multiline:
      line1
      line2

    multiline2:

      line3
      line4

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
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 22 lines)
Updated from source of 300 bytes, 23 lines
Resulted in 300 bytes, 27 lines

\tAdded from new file:
\t\tobject/multiline2                        5  | multiline2:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }

    def "Check multiline values negative shift merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/multiline_upd.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/multiline.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        // note multilines reverse order is not a bug! there is no way to order it differently
        unifyString(current.text) == """object:
  simple: value with
      multiple lines (flow)

  multiline2:

    line3
    line4

  multiline:
    line1
    line2

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

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 23 lines)
Updated from source of 300 bytes, 22 lines
Resulted in 300 bytes, 27 lines

\tAdded from new file:
\t\tobject/multiline                         5  | multiline:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check lists merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/lists.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/lists_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# explicitly shifted lines

simple_list:
  - one
  # comment
  - three

# new comment appear
object:
  - one: 1
    two: 2


object2:
  -
    one: 1
    two: 2


object3:
  - one: 1
    two:
      three: 3
      four: 4
    and:
      - sub1
      - sub2


map_of_maps:
  one:
    a1: 1
    a2: 2
  two:
    b1: 1
    b2: 2

empty:
  -
    one: 1
    two: 2
    three: 3

reorder:
  - one: 1
    three: 3
    two: 2

complexMatch:
  - one: 1
    two: 2
    three: 4

sublist:
  - one:
      sub1: 1
      sub2: 2

emptyLine:

  - one

quoted:
  - "name": value
"""

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 48 lines)
Updated from source of 300 bytes, 71 lines
Resulted in 300 bytes, 64 lines

\tAdded from new file:
\t\tobject[0]/two                            17 | two: 2
\t\tobject2[0]/two                           26 | two: 2
\t\tobject3[0]/two/four                      37 | four: 4
\t\tobject3[0]/and                           38 | and:
\t\tmap_of_maps/one/a2                       46 | a2: 2
\t\tmap_of_maps/two                          47 | two:
\t\tcomplexMatch[0]/three                    66 | three: 4
\t\tsublist[0]/one/sub2                      71 | sub2: 2
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }

    def "Check lists merge reversed"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/lists_upd.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/lists.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# explicitly shifted lines

simple_list:
  - one

  # comment
  - two
  # comment
  - three
  - 'prop: like'
  - multiline
    property

object:
  - one: 1
    two: 2
    # shifted comment
  - one: 1.1
    two: 2.2


object2:
  -
    one: 1
    two: 2
    # shifted comment
  -
    one: 1.1
    two: 2.2


object3:
  - one: 1
    two:
      three: 3
      four: 4
    and:
      - sub1
      - sub2


map_of_maps:
  one:
    a1: 1
    a2: 2
  two:
    b1: 1
    b2: 2

empty:
  - one: 1
    two: 2
    three: 3

reorder:
  - two: 2
    one: 1
    three: 3

complexMatch:
  - one: 1
    two: 3
    three: 3
  - one: 1
    two: 2
    three: 4

sublist:
  - one:
      sub1: 1
      sub2: 2

emptyLine:

  - one

quoted:
  - "name": value
"""

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 71 lines)
Updated from source of 300 bytes, 48 lines
Resulted in 300 bytes, 78 lines

\tAdded from new file:
\t\tempty[0]/three                           28 | three: 3
\t\treorder[0]/three                         33 | three: 3
\t\temptyLine                                43 | emptyLine:
\t\tquoted                                   47 | quoted:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }

    def "Check configuration not changed"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2

prop2:

  # sub comment
  prop2.1: 2.1

  list:
    - one
    - two

  obj:
    - one: 1
      two: 2

# original comment
pppp: some"""
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 23 lines)

\tNot changed
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }
}
