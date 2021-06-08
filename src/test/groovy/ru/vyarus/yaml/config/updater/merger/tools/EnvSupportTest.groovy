package ru.vyarus.yaml.config.updater.merger.tools

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 08.06.2021
 */
class EnvSupportTest extends Specification {

    def "Check variables appliance"() {

        expect: "vars replaced"
        EnvSupport.apply("""
sample #{var1} or
another #{var2}
""", ['var1': '11', 'var2': '12']) == """
sample 11 or
another 12
"""

        and: "unknown vars remain"
        EnvSupport.apply("""
sample #{var1} or
another #{var2}
""", ['var1': '11']) == """
sample 11 or
another #{var2}
"""
    }
}
