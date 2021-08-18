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
     * <p>
     * If you going to modify tree then modify both trees accordingly (otherwise final validation would fail)
     *
     * @param tree   comments tree
     * @param struct structure tree
     * @see ru.vyarus.yaml.updater.parse.comments.model.CmtNodeFactory for creating new comment nodes
     * @see ru.vyarus.yaml.updater.parse.struct.model.StructNodeFactory for creating new struct nodes
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
     * <p>
     * If you going to modify tree then modify both trees accordingly (otherwise final validation would fail)
     *
     * @param tree   comments tree
     * @param struct structure tree
     * @see ru.vyarus.yaml.updater.parse.comments.model.CmtNodeFactory for creating new comment nodes
     * @see ru.vyarus.yaml.updater.parse.struct.model.StructNodeFactory for creating new struct nodes
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
     * @see ru.vyarus.yaml.updater.parse.comments.model.CmtNodeFactory for creating new comment nodes
     */
    void merged(CmtTree result);

    /**
     * Called after result validation. Not called if validation disabled.
     * <p>
     * At this point merged file was already saved. Passed model is a read of just merged file for validation.
     * Any changes to this model will not take any effect. Method may be used for additional validations.
     *
     * @param result parsed merged file (with snakeyaml)
     */
    void validated(StructTree result);

    /**
     * Called after backup file creation (only if required).
     *
     * @param backup created backup file for current configuration
     */
    void backupCreated(File backup);
}
