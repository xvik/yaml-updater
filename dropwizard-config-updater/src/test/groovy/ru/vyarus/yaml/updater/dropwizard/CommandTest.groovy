package ru.vyarus.yaml.updater.dropwizard

/**
 * @author Vyacheslav Rusakov
 * @since 23.07.2021
 */
class CommandTest extends AbstractTest {

    def "Check simple merge"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_upd.yml", "-i")

        then: "merged"
        isBackupCreated()
        res == """# something

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

    def "Check merge from classpath"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "/simple_upd.yml")

        then: "merged"
        isBackupCreated()
        res == """# something

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

    def "Check props remove"() {

        when: "perform merge with props remove"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_upd.yml",
                "-r", "prop1", "prop2/list")

        then: "merged"
        res == """# something

# something 2
prop1:
  prop1.1: 2

  prop1.2: 3
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
    }


    def "Check no backup and no validation"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_upd.yml", "-bv")

        then: "merged"
        !isBackupCreated()
        res == """# something

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
