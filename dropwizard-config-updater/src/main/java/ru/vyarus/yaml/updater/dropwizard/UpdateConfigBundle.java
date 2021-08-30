package ru.vyarus.yaml.updater.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ru.vyarus.yaml.updater.dropwizard.cmd.UpdateConfigCommand;

/**
 * Registers {@link ru.vyarus.yaml.updater.dropwizard.cmd.UpdateConfigCommand}. Required to update application
 * configuration from config in new delivery (add missed properties, update comments, reformat if required).
 * <p>
 * Default usage: {@code java -jar project.jar update-config current.yml new.yml}.
 * Available options: {@code java -jar project.jar update-config -h}.
 *
 * @author Vyacheslav Rusakov
 * @since 15.07.2021
 */
public class UpdateConfigBundle implements ConfiguredBundle<Configuration> {

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        bootstrap.addCommand(new UpdateConfigCommand());
    }

    // added for dropwizard 1 compatibility
    @Override
    public void run(final Configuration configuration, final Environment environment) throws Exception {
        // empty
    }
}
