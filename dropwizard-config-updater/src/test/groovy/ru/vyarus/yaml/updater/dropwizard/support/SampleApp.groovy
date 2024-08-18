package ru.vyarus.yaml.updater.dropwizard.support

import io.dropwizard.core.Application
import io.dropwizard.core.Configuration
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import ru.vyarus.yaml.updater.dropwizard.UpdateConfigBundle

/**
 * @author Vyacheslav Rusakov
 * @since 23.07.2021
 */
class SampleApp extends Application<Configuration> {

    @Override
    void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new UpdateConfigBundle())
    }

    @Override
    void run(Configuration configuration, Environment environment) throws Exception {
    }
}
