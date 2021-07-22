package ru.vyarus.yaml.updater.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import ru.vyarus.yaml.updater.dropwizard.cmd.UpdateConfigCommand;

/**
 * @author Vyacheslav Rusakov
 * @since 15.07.2021
 */
public class ConfigUpdaterBundle implements ConfiguredBundle<Configuration> {

    @Override
    public void initialize(final Bootstrap<?> bootstrap) {
        bootstrap.addCommand(new UpdateConfigCommand());
    }
}
