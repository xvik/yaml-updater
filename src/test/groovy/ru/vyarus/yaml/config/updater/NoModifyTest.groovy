package ru.vyarus.yaml.config.updater

import com.amihaiemil.eoyaml.Yaml
import com.amihaiemil.eoyaml.YamlMapping
import org.junit.Test
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
class NoModifyTest extends Specification {

    @Test
    def "check read write"() {
        String sample = """

# comment 1

# comment 2

# relative comment
foo:
  bar: 11  
  #cmt1
  baz: 12
  
# orphan comment   

"""

        YamlMapping yaml = Yaml.createYamlInput(sample).readYamlMapping()

        expect:
        yaml.toString() == sample
    }
}
