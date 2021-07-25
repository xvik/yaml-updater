package ru.vyarus.yaml.updater.dropwizard

/**
 * @author Vyacheslav Rusakov
 * @since 25.07.2021
 */
class VariablesTest extends AbstractTest {

    def "Check direct variables"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml",
                "-e", "var1=tt", "var2=pp")

        then: "merged"
        res == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2
  prop2: tt
  prop3: pp
  prop4: #{var3}

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

    def "Check variables from file"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml",
                "-e", "src/test/resources/vars.properties")

        then: "merged"
        res == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2
  prop2: tt
  prop3: pp
  prop4: #{var3}

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

    def "Check variables from classpath"() {

        when: "perform merge"
        def res = run("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml",
                "-e", "/vars.properties", "var3=test")

        then: "merged"
        res == """# something

# something 2
prop1:
  prop1.1: 1.1

  prop1.2: 1.2
  prop2: tt
  prop3: pp
  prop4: test

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
