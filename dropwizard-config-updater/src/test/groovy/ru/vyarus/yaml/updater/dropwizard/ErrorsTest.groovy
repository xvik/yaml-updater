package ru.vyarus.yaml.updater.dropwizard

/**
 * @author Vyacheslav Rusakov
 * @since 27.08.2021
 */
class ErrorsTest extends AbstractTest {

    def "Check invalid file url"() {

        expect: "invalid file url"
        def res = runWithError("config.yml", "http://localhost/file.yml")
        // different on lin and win
        res.contains("Update file not found: http://localhost/file.yml") || res.contains("Failed to load file from url: http://localhost/file.yml")

    }

    def "Check not existing file"() {

        expect: "invalid file path"
        runWithError("config.yml", "file.yml")
                .contains("Update file not found: file.yml")
    }

    def "Check variables file not found"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "file:vars.properties")
                .contains("Variables file not found: file:vars.properties")
    }

    def "Check bad variables file"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "src/test/resources/bad.properties")
                .contains("Failed to read variables from: src/test/resources/bad.properties")
    }

    def "Check bad variable declaration"() {

        expect: "no variables file"
        runWithError("src/test/resources/simple.yml", "src/test/resources/simple_vars.yml", "-e", "=val")
                .contains("Invalid variable declaration: =val")
    }
}
