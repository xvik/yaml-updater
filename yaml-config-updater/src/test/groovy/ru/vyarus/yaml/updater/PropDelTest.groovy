package ru.vyarus.yaml.updater

import ru.vyarus.yaml.updater.report.ReportPrinter

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
class PropDelTest extends AbstractTest {

    def "Check property remove"() {
        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).deleteProps('prop2/list').update()

        then: "list replaced"
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
        ReportPrinter.print(report) == """Configuration: /tmp/CONFIG.yml (198 bytes, 23 lines)
Updated from source of 385 bytes, 40 lines
Resulted in 343 bytes, 37 lines

\tRemoved from old file:
\t\tprop2/list                               14 | list:

\tAdded from new file:
\t\tprop1/prop1.3                            9  | prop1.3: 1.3
\t\tprop11                                   12 | prop11:
\t\tprop2/list                               20 | list:
\t\tprop2/obj[0]/three                       31 | three: 3
\t\tprop3                                    39 | prop3:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check delete fallback"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "deleting property with dot as separator"
        def report = YamlUpdater.create(current, update).backup(false).deleteProps('prop2.list').update()

        then: "list replaced"
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
        ReportPrinter.print(report) == """Configuration: /tmp/CONFIG.yml (198 bytes, 23 lines)
Updated from source of 385 bytes, 40 lines
Resulted in 343 bytes, 37 lines

\tRemoved from old file:
\t\tprop2/list                               14 | list:

\tAdded from new file:
\t\tprop1/prop1.3                            9  | prop1.3: 1.3
\t\tprop11                                   12 | prop11:
\t\tprop2/list                               20 | list:
\t\tprop2/obj[0]/three                       31 | three: 3
\t\tprop3                                    39 | prop3:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check unknown property remove"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "deleting unknown property"
        def report = YamlUpdater.create(current, update).backup(false).deleteProps('prop23.list').update()

        then: "list replaced"
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
        ReportPrinter.print(report) == """Configuration: /tmp/CONFIG.yml (198 bytes, 23 lines)
Updated from source of 385 bytes, 40 lines
Resulted in 331 bytes, 36 lines

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
}
