package ru.vyarus.yaml.updater

/**
 * @author Vyacheslav Rusakov
 * @since 19.11.2021
 */
class QuotesUpdateTest extends AbstractTest {

    def "Check quites merge"() {

        when: "merging"
        def report = YamlUpdater.createTest('/merge/quotes.yml', '/merge/quotes_upd.yml')
                .printReport(false)
                .update()

        then: "updated"
        unifyString(report.dryRunResult) == """
'double': 1
"single": 2

complex:name: 3
"'complex':name2": 4
'"complex":name3': 5
smth's:name: 6

"compl:\\ ex": 7
"cmpl:\\u0020x": 8

list:
  - something: val1
  - "cm:\\u0020g": val2
  - '1 - création d''en': val3
"""

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 15 lines)
Updated from source of 300 bytes, 14 lines
Resulted in 300 bytes, 16 lines

\tAdded from new file:
\t\tcmpl: x                                  9  | "cmpl:\\u0020x": 8
""".replace("/tmp/CONFIG.yml", report.config.absolutePath)

    }

    def "Check quites merge reversed"() {

        when: "merging"
        def report = YamlUpdater.createTest('/merge/quotes_upd.yml', '/merge/quotes.yml')
                .printReport(false)
                .update()

        then: "updated"
        unifyString(report.dryRunResult) == """
"double": 1
'single': 2

"complex:name": 3
"'complex':name2": 4
'"complex":name3': 5
'smth''s:name': 5

"compl:\\ ex": 7
"cmpl:\\u0020x": 8

list:
  - "something": val1
  - "cm:\\u0020g": val2
  - "1 - création d'en": val3
"""

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 14 lines)
Updated from source of 300 bytes, 15 lines
Resulted in 300 bytes, 16 lines

\tAdded from new file:
\t\t'complex':name2                          6  | "'complex':name2": 4
\t\t"complex":name3                          7  | '"complex":name3': 5
""".replace("/tmp/CONFIG.yml", report.config.absolutePath)

    }

    def "Check quoted property remove"() {

        when: "merging"
        def report = YamlUpdater.createTest('/merge/quotes.yml', '/merge/quotes_upd.yml')
                .deleteProps("double", "'complex':name2", "list[1].\"cm:\\u0020g\"")
                .printReport(false)
                .update()

        then: "updated"
        unifyString(report.dryRunResult) == """
'double': 1
"single": 2

complex:name: 3
'"complex":name3': 5
smth's:name: 6

"compl:\\ ex": 7
"cmpl:\\u0020x": 8

list:
  - something: val1
  - '1 - création d''en': val3
"""

        and: "report correct"
        print(report) == """Configuration: /tmp/CONFIG.yml (300 bytes, 15 lines)
Updated from source of 300 bytes, 14 lines
Resulted in 300 bytes, 14 lines

\tRemoved from old file:
\t\tdouble                                   2  | "double": 1
\t\t'complex':name2                          6  | "'complex':name2": 4
\t\tlist[-1]                                 14 | - 

\tAdded from new file:
\t\tdouble                                   2  | 'double': 1
\t\tcmpl: x                                  9  | "cmpl:\\u0020x": 8
""".replace("/tmp/CONFIG.yml", report.config.absolutePath)

    }
}
