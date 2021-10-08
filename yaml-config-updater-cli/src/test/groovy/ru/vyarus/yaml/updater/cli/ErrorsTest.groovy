package ru.vyarus.yaml.updater.cli

/**
 * @author Vyacheslav Rusakov
 * @since 27.08.2021
 */
class ErrorsTest extends AbstractTest {

    def "Check invalid file url"() {

        expect: "invalid file url"
        runWithError("config.yml", "http://localhost/file.yml")
                .contains("Invalid update file: http://localhost/file.yml")

    }

    def "Check not existing file"() {

        expect: "invalid file path"
        runWithError("config.yml", "file.yml")
                .contains("Invalid update file (does not exists): file.yml")
    }

    def "Check variables file not found"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "file:vars.properties")
                .contains("Invalid variables file (does not exists): file:vars.properties")
    }

    def "Check bad variables file"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "src/test/resources/bad.properties")
                .contains("Invalid variables file: src/test/resources/bad.properties (Malformed \\uxxxx encoding.)")
    }

    def "Check bad variable declaration"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "=val")
                .contains("Invalid variable declaration: =val")
    }
}
