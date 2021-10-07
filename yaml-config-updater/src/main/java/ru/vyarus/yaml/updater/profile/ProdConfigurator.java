package ru.vyarus.yaml.updater.profile;

import ru.vyarus.yaml.updater.UpdateConfig;

import java.io.File;
import java.io.InputStream;

/**
 * Represents normal tool run.
 * <p>
 * Class required just to avoid warnings in case of using directly parametrized class (and to make
 * {@link ru.vyarus.yaml.updater.YamlUpdater} constructors more readable.
 */
public class ProdConfigurator extends UpdateConfig.Configurator<ProdConfigurator> {

    public ProdConfigurator(final File current, final InputStream update) {
        super(current, update);
    }
}
