package ru.vyarus.yaml.updater.cases

import ru.vyarus.yaml.updater.AbstractTest

/**
 * @author Vyacheslav Rusakov
 * @since 16.11.2021
 */
class PropertyRecognitionTest extends AbstractTest {

    def "Check incorrect property recognition"() {

        when: "merging file without space after property name"
        def report = createQuickTest(
                // src
                """
list:
    # scalar
    - not:a:prop
""",

                // target
                """
real:name: val
""")
                .update()

        then: "list item not recognized as property"
        unifyString(report.dryRunResult) == """
list:
    # scalar
    - not:a:prop

real:name: val
"""
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 4 lines)
Updated from source of 300 bytes, 2 lines
Resulted in 300 bytes, 6 lines

\tAdded from new file:
\t\treal:name                                2  | real:name: val
""".replace("/tmp/CONFIG.yml", report.config.getAbsolutePath())
    }
}
