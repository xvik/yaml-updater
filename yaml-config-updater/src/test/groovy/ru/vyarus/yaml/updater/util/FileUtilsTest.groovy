package ru.vyarus.yaml.updater.util

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 08.10.2021
 */
class FileUtilsTest extends Specification {

    def "Check file lookup"() {

        expect: "load by relative path"
        FileUtils.findFile('src/test/resources/common/sample.yml') != null
        FileUtils.findFile('src/test/resources/common/bad.yml') == null

        and: "load by absolute path"
        FileUtils.findFile(new File('src/test/resources/common/sample.yml').absolutePath) != null
        FileUtils.findFile(new File('src/test/resources/common/sample.yml').parentFile.absolutePath
                + File.separator + 'bad.yml') == null

        and: "load from classpath"
        FileUtils.findFile('/common/sample.yml') != null
        FileUtils.findFile('/common/bad.yml') == null

        and: "load by url"
        FileUtils.findFile('file:src/test/resources/common/sample.yml') != null
        FileUtils.findFile('file:src/test/resources/common/bad.yml') == null
    }

    def "Check find existing file"() {

        when: "trying not existing file"
        FileUtils.findExistingFile('bad.yml')

        then: "error"
        thrown(IllegalArgumentException)
    }

    def "Check read content"() {

        String content = new File('src/test/resources/common/sample.yml').text

        expect: "file read ok"
        FileUtils.read(FileUtils.findFile('src/test/resources/common/sample.yml')) == content

        and: "classpath read ok"
        FileUtils.read(FileUtils.findFile('/common/sample.yml')) == content

        and: "url read ok"
        FileUtils.read(FileUtils.findFile('file:src/test/resources/common/sample.yml')) == content
    }

    def "Check tmp file creation"() {

        when: "creating tmp file"
        String content = new File('src/test/resources/common/sample.yml').text
        File tmp = FileUtils.copyToTempFile('src/test/resources/common/sample.yml')

        then: "ok"
        tmp.exists()
        tmp.text == content

        cleanup:
        tmp?.delete()
    }

    def "Check properties load"() {

        when: "loading not existing"
        Map res = FileUtils.loadProperties('bad.yml')

        then: "empty"
        res.isEmpty()

        when: "loading normal props"
        res = FileUtils.loadProperties('/prop/test.properties')

        then: "ok"
        res.size() == 2
        res['foo'] == 'bar'

        when: "loading bad props"
        FileUtils.loadProperties('/prop/badtest.properties')

        then: "error"
        thrown(IllegalStateException)
    }
}
