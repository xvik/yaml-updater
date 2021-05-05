package ru.vyarus.yaml.config.updater.struct

import org.yaml.snakeyaml.Yaml
import ru.vyarus.yaml.config.updater.struct.model.YamlStructTree
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 05.05.2021
 */
class StructParseTest extends Specification {

    def "Check simple parse"() {

        when: "parsing"
        YamlStructTree tree = StructureReader.read(new File(getClass().getResource('/comments/sample.yml').toURI()))
        println tree
        
        then: "ok"
        tree.nodes.size() == 3
    }
}
