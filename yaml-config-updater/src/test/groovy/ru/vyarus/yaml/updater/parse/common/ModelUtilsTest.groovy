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

    def "Check property cleanup"() {

        expect:
        YamlModelUtils.cleanPropertyName(prop) == result

        where:
        prop           | result
        'prop'         | 'prop'
        '"prop"'       | 'prop'
        '\'prop\''     | 'prop'
        "'smth''ng'"   | "smth'ng"
        '"s\"t"'       | 's"t'
        '"s\\"t"'      | 's"t'
        '"s:\\ n"'     | 's: n'
        '"s:\\u0020n"' | 's: n'
    }

    def "Check path cleanup"() {

        expect:
        YamlModelUtils.cleanPropertyPath(path, dot) == result

        where:
        path                      | dot   | result
        'prop'                    | true  | 'prop'
        'prop/name'               | false | 'prop/name'
        'prop.name'               | true  | 'prop/name'
        'prop."name"'             | true  | 'prop/name'
        'prop."name".bar'         | true  | 'prop/name/bar'
        'prop/"name"/bar'         | false | 'prop/name/bar'
        'prop."na.me".bar'        | true  | 'prop/na.me/bar'
        "prop.'na.me'.bar"        | true  | 'prop/na.me/bar'
        'prop."na.\\u0020me".bar' | true  | 'prop/na. me/bar'

    }
}
