package ru.vyarus.yaml.updater.update


import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
class EnvSupportTest extends Specification {

    def "Check variables appliance"() {

        expect: "vars replaced"
        new EnvSupport(['var1': '11', 'var2': '12']).apply("""
sample #{var1} or
another #{var2}
""") == """
sample 11 or
another 12
"""

        and: "unknown vars remain"
        new EnvSupport(['var1': '11']).apply("""
sample #{var1} or
another #{var2}
""") == """
sample 11 or
another #{var2}
"""
    }
}
