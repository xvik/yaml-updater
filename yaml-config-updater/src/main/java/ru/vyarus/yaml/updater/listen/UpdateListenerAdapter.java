package ru.vyarus.yaml.updater.listen;

import ru.vyarus.yaml.updater.UpdateConfig;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;

import java.io.File;

/**
 * Dummy implementation of {@link ru.vyarus.yaml.updater.listen.UpdateListener} to simplify selective implementation.
 * Also used when no listener configured.
 *
 * @author Vyacheslav Rusakov
 * @since 17.08.2021
 */
public class UpdateListenerAdapter implements UpdateListener {

    @Override
    public void configured(final UpdateConfig config) {
        // default no action
    }

    @Override
    public void updateConfigParsed(final CmtTree tree, final StructTree struct) {
        // default no action
    }

    @Override
    public void currentConfigParsed(final CmtTree tree, final StructTree struct) {
        // default no action
    }

    @Override
    public void merged(final CmtTree result) {
        // default no action
    }

    @Override
    public void backupCreated(final File backup) {
        // default no action
    }
}
