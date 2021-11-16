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

    def "Check quoted property recognition"() {

        when: "merging file with quoted properties"
        def report = createQuickTest(
                // src
                """
"prop1": 1
'prop2': 2
""",

                // target
                """
"complex:name": 3
"'complex':name2": 4
'smth''s:name': 5
""")
                .update()

        then: "properties recognized"
        unifyString(report.dryRunResult) == """
"prop1": 1
'prop2': 2

"complex:name": 3
"'complex':name2": 4
'smth''s:name': 5
"""
        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 3 lines)
Updated from source of 300 bytes, 4 lines
Resulted in 300 bytes, 7 lines

\tAdded from new file:
\t\tcomplex:name                             2  | "complex:name": 3
\t\t'complex':name2                          3  | "'complex':name2": 4
\t\tsmth's:name                              4  | 'smth''s:name': 5
""".replace("/tmp/CONFIG.yml", report.config.getAbsolutePath())
    }
}
