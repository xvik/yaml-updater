package ru.vyarus.yaml.updater.dropwizard.support

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.yaml.updater.dropwizard.ConfigUpdaterBundle

/**
 * @author Vyacheslav Rusakov
 * @since 23.07.2021
 */
class SampleApp extends Application<Configuration> {

    @Override
    void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new ConfigUpdaterBundle())
    }

    @Override
    void run(Configuration configuration, Environment environment) throws Exception {
    }
}
