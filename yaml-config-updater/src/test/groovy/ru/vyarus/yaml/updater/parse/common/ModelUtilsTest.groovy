package ru.vyarus.yaml.updater.parse.common

import ru.vyarus.yaml.updater.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 21.08.2021
 */
class ModelUtilsTest extends AbstractTest {

    def "Check path reducing"() {

        expect:
        YamlModelUtils.removeLeadingPath(cut, path) == result

        where:
        cut   | path      | result
        null  | 'some'    | 'some'
        ''    | 'some'    | 'some'
        'one' | 'one/two' | 'two'
        'one' | 'one[10]' | '[10]'
    }
}
