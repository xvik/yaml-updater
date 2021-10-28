package ru.vyarus.yaml.updater.dropwizard

import ru.vyarus.yaml.updater.dropwizard.support.SampleApp

/**
 * @author Vyacheslav Rusakov
 * @since 29.10.2021
 */
class SameDirConfigTest extends AbstractTest {

    def "Check same dir config"() {

        setup:
        // yes test creates file at project root! but it is the only chance to test direct file case
        File cfg = new File('config.yml')
        assert !cfg.exists()

        when: "run update for cfg relative to root"
        new SampleApp().run('update-config', cfg.name, 'src/test/resources/simple.yml')

        then: "everything is ok"
        cfg.exists()

        cleanup:
        cfg.delete()
    }

    def "Check same dir config backup"() {

        setup:
        // yes test creates file at project root! but it is the only chance to test direct file case
        File cfg = new File('config.yml')
        assert !cfg.exists()
        cfg << '#nothing'

        when: "run update for cfg relative to root"
        new SampleApp().run('update-config', cfg.name, 'src/test/resources/simple.yml')

        then: "everything is ok"
        cfg.length() > 0

        cleanup:
        cfg.absoluteFile.parentFile.eachFile {
            if (it.name.startsWith('config.yml')) {
                it.delete()
            }
        }
    }
}
