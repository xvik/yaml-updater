package ru.vyarus.yaml.updater

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Vyacheslav Rusakov
 * @since 05.10.2021
 */
class UpdateFlowPropsTest extends AbstractTest {

    def "Check sequences merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/sequences.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/sequences_upd.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """line: [1, 2, 4]

multiLine: [1, 2,
            3, 4]

empty: []

object: { one: 1, two: 2 }

multiObject: { one: 1,
               two: 2 }

emptyObj: {}

listOfArr:
  - [1, 2]
  - []

listOfObj:
  - { one: 1, two: 3 }
  - {}

addition: [2, 3]

addition2: {t: t}
"""
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 21 lines)
Updated from source of 300 bytes, 27 lines
Resulted in 300 bytes, 25 lines

\tAdded from new file:
\t\taddition                                 25 | addition: [2, 3]
\t\taddition2                                27 | addition2: {t: t}
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }


    def "Check sequences reverse merge"() {

        setup: "prepare files"
        File current = Files.createTempFile("config", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/sequences_upd.yml').toURI()).toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File update = Files.createTempFile("update", ".yml").toFile()
        Files.copy(new File(getClass().getResource('/merge/sequences.yml').toURI()).toPath(), update.toPath(), StandardCopyOption.REPLACE_EXISTING)

        when: "merging"
        def report = YamlUpdater.create(current, update).backup(false).update()

        then: "updated"
        unifyString(current.text) == """line: [1, 2]

multiLine: [1, 2,
            3]

empty: [1]

object: { one: 1}

multiObject: {
               two: 2 }

emptyObj: {o: 1}

listOfArr:
  - [1]
  - [2]
  - []

listOfObj:
  - { one: 1}
  - { two: 3 }
  - {}

addition: [2, 3]

addition2: {t: t}
"""
        and: "report correct"
        print(report) == """Configuration not changed: /tmp/CONFIG.yml (300 bytes, 27 lines)
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())

        cleanup:
        current.delete()
        update.delete()
    }
}
