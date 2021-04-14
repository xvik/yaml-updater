package ru.vyarus.yaml.config.updater.merger;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Merger {

    private final MergerConfig config;

    public Merger(MergerConfig config) {
        this.config = config;
    }

    public void execute() {
        File source = config.getSource();
        if (config.getEnv() != null) {
            
        }
    }
}
