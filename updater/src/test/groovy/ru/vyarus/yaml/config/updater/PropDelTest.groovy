package ru.vyarus.yaml.config.updater


import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
class PropDelTest extends AbstractTest {

    def "Check simple merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        YamlUpdater.configure(current, update).backup(false).deleteProps('prop2/list').create().execute()

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

        cleanup:
        current.delete()
        update.delete()
    }

}
