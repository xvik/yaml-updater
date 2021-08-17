package ru.vyarus.yaml.updater.listen;

import ru.vyarus.yaml.updater.UpdateConfig;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;

import java.io.File;

/**
 * Update stages listener. Provide hooks for the main processing points and opens internal model. Used mainly for
 * testing (to simulate situations), but might be used as a workaround for specific cases (merger bugs workaround or
 * manual model processing).
 *
 * @author Vyacheslav Rusakov
 * @see ru.vyarus.yaml.updater.listen.UpdateListenerAdapter
 * @since 17.08.2021
 */
public interface UpdateListener {

    /**
     * Called just before start to provide access to configuration actually used by merger (catching configuration
     * errors).
     *
     * @param config configuration instance
     */
    void configured(UpdateConfig config);

    /**
     * Parsed new configuration with two parsers (preserving comments and snakeyaml). At this point trees were
     * validated to be equal (merger self-control mechanism).
     * <p>
     * Environment variables already applied.
     * <p>
     * Passed structure tree would be used later for the final result validation, so be careful with modifications.
     *
     * @param tree   comments tree
     * @param struct structure tree
     */
    void updateConfigParsed(CmtTree tree, StructTree struct);

    /**
     * Parsed current configuration with two parsers (preserving comments and snakeyaml). At this point trees were
     * validated to be equal (merger self-control mechanism).
     * <p>
     * Configured deletions already applied.
     * <p>
     * Method would not be called if current configuration does not exist.
     * <p>
     * Passed structure tree would be used later for the final result validation, so be careful with modifications.
     * <p>
     * NOTE: The same {@link ru.vyarus.yaml.updater.parse.comments.model.CmtTree} instance used for updates and
     * would be written to file after merge.
     *
     * @param tree   comments tree
     * @param struct structure tree
     */
    void currentConfigParsed(CmtTree tree, StructTree struct);

    /**
     * Called just after the merge, but before final validation. Provided structure would be written to file
     * after validation.
     * <p>
     * NOTE: validation would detect modifications and deny entire update. If you want to manually correct the resulted
     * tree then disable result validation (it would still read the resulted file to make sure it's semantically
     * correct, but would not compare with old/new files).
     *
     * @param result merge result
     */
    void merged(CmtTree result);

    /**
     * Called after backup file creation (only if required).
     *
     * @param backup created backup file for current configuration
     */
    void backupCreated(File backup);
}
