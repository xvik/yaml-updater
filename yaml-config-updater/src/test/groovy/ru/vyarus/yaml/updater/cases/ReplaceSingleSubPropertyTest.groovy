package ru.vyarus.yaml.updater.cases

import ru.vyarus.yaml.updater.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 11.11.2021
 */
class ReplaceSingleSubPropertyTest extends AbstractTest {

    def "Check sub property replacing"() {

        when: "merging file with space after property name"
        def report = createQuickTest(
                // src
                """
prop:
    sub: 1
""",

                // target
                """
prop:
    sub: 2
""")
                .deleteProps('prop.sub')
                .update()

        then: "whitespace in names removed"
        unifyString(report.dryRunResult) == """
prop:
    sub: 2
"""
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 3 lines)
Updated from source of 300 bytes, 3 lines
Resulted in 300 bytes, 3 lines

\tRemoved from old file:
\t\tprop                                     2  | prop:

\tAdded from new file:
\t\tprop                                     2  | prop:
""".replace("/tmp/CONFIG.yml", report.config.getAbsolutePath())
    }
}
