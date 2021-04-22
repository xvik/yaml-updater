package ru.vyarus.yaml.config.updater.merger;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Merger {

    private final MergerConfig config;
    private File backup;
    private File update;

    public Merger(MergerConfig config) {
        this.config = config;
    }

    public void execute() {
        try {
            backup();
            prepareNewConfig();
            merge();
            validateResult();
        } catch (Exception ex) {
            rollback();
            throw new IllegalStateException("Failed to update: original configuration remains", ex);
        } finally {
            cleanup();
        }
    }

    private void backup() {

    }

    private void prepareNewConfig() {

    }

    private void merge() {

    }

    private void validateResult() {

    }

    private void cleanup() {

    }

    private void rollback() {

    }
}
