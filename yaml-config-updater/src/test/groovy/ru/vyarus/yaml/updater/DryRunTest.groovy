package ru.vyarus.yaml.updater
/**
 * @author Vyacheslav Rusakov
 * @since 06.10.2021
 */
class DryRunTest extends AbstractTest {

    def "Check simple merge"() {

        when: "merging"

        def current = new File(getClass().getResource('/merge/simple.yml').toURI())
        def report = YamlUpdater.create(
                current,
                getClass().getResourceAsStream('/merge/simple_upd.yml'))
                .backup(true) // make sure backup ignored
                .dryRun(true)
                .update()

        then: "updated"
        report.dryRun
        // files size would be different here because afterSize obtained with File.length() which may vary between OS
        report.dryRunResult.split(System.lineSeparator()).size() == report.afterLinesCnt
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
Updated from source of 300 bytes, 40 lines
Resulted in 300 bytes, 36 lines

\tAdded from new file:
\t\tprop1/prop1.3                            9  | prop1.3: 1.3
\t\tprop11                                   12 | prop11:
\t\tprop2/obj[0]/three                       31 | three: 3
\t\tprop3                                    39 | prop3:
""".replace("/tmp/CONFIG.yml", current.getAbsolutePath())
    }
}
