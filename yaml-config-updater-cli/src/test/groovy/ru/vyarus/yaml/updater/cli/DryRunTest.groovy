package ru.vyarus.yaml.updater.cli

/**
 * @author Vyacheslav Rusakov
 * @since 06.10.2021
 */
class DryRunTest extends AbstractTest {

    def "Check simple merge"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_upd.yml", "--dry-run")

        then: "not changed"
        res == """# something

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
    }
}
