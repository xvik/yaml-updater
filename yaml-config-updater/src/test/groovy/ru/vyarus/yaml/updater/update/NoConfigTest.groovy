package ru.vyarus.yaml.updater.update

import ru.vyarus.yaml.updater.AbstractTest
import ru.vyarus.yaml.updater.YamlUpdater

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 17.08.2021
 */
class NoConfigTest extends AbstractTest {

    def "Check first config install"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        current.delete()

        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/simple_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "updating not existing config"
        YamlUpdater.create(current, update).backup(false).update()

        then: "config copied"
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
