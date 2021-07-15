package ru.vyarus.yaml.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.updater.parse.comments.CommentsReader;
import ru.vyarus.yaml.updater.parse.comments.CommentsWriter;
import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.updater.parse.struct.StructureReader;
import ru.vyarus.yaml.updater.parse.struct.model.StructNode;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;
import ru.vyarus.yaml.updater.update.CommentsParserValidator;
import ru.vyarus.yaml.updater.update.EnvSupport;
import ru.vyarus.yaml.updater.update.TreeMerger;
import ru.vyarus.yaml.updater.update.UpdateResultValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Yaml configuration merger preserving comments. Use two yaml parsers: snakeyaml for self-validation and
 * custom one collecting comments (it cares about structure only, not parsing actual values).
 * Bu default, this custom parser could read and write yaml file without changing anything.
 * <p>
 * Comments parsing logic: everything above property is a property comment (comments and empty lines between comments).
 * If yaml file has a large header - it would be assumed as first property comment. If there is a footer comment
 * (after all properties) - it would be also preserved. Value parts stored as is so if there was a side comment,
 * it would also be preserved (including multi-line values).
 * <p>
 * There are three steps of validation with snakeyaml:
 * - first, each file (current and update) configs read by both parsers and validated for structural identity
 * (prevent parse errors)
 * - merged result is also read by snakeyaml and all values compared with old and update files (to make sure
 * resulted file is valid and values were merged correctly)
 * <p>
 * Updating file may come from any source: fs file, classpath, url (you just need to prepare correct stream).
 * <p>
 * Environment variables could be used for personalizing update file for current environment (note that variables
 * syntax is {@code #{var}} because {@code ${var}} could be used by config natively and hash-based placeholders
 * in values would be treated as comments (leaving value empty on parse).
 * <p>
 * Merge rules:
 * - All yaml nodes presented in current config will remain, but comments might be updated (if matching node found
 * in update file).
 * - All new properties copied from update file.
 * - Update file's properties order used (so if in current and update file the same properties would be used,
 * but order changed - update file order would be applied).
 * - Properties padding taken from update file. For example, if in current file properties were shifted with two spaced
 * and in update file with 4 then all properties would be shifted according to update file (even if no new properties
 * applied). Shift appear on subtree level (where subtrees could be matched) so if there are subtrees in old file
 * not present in new one - old paddings will remain there (no target to align by).
 * - Lists are not merged. But if list contain object items, such items are updated (new properties added).
 * Items matched by property values.
 * <p>
 * To remove not needed properties or to override existing value provide list of yaml paths to remove.
 * Note that path elements separated with '/' because dot is valid symbol for property name.
 *
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class YamlUpdater {
    private final Logger logger = LoggerFactory.getLogger(YamlUpdater.class);

    private final UpdateConfig config;
    // merge result until final validation (tmp file)
    private File work;
    private StructTree currentStructure;
    private CmtTree currentTree;

    private StructTree updateStructure;
    private CmtTree updateTree;

    YamlUpdater(final UpdateConfig config) {
        this.config = config;
    }

    /**
     * Shortcut for {@link #configure(File, InputStream)}.
     *
     * @param current config file to be updated
     * @param update  update file
     * @return builder instance for chained calls
     */
    public static UpdateConfig.Configurator configure(final File current, final File update) {
        try {
            return configure(current, Files.newInputStream(update.toPath()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Update file '" + update.getAbsolutePath() + "' not found", e);
        }
    }

    /**
     * Builds updater configurator. Update file might be physical file, classpath resource, remote url or whatever else.
     * <p>
     * Stream closed after content reading.
     *
     * @param current config file to be updated
     * @param update  update file content
     * @return builder instance for chained calls
     * @see #configure(File, File) shortcut for direct file case (most common)
     */
    public static UpdateConfig.Configurator configure(final File current, final InputStream update) {
        return UpdateConfig.configureUpdate(current, update);
    }

    public void execute() {
        try {
            prepareNewConfig();
            prepareCurrentConfig();
            merge();
            validateResult();
            backupAndReplace();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to update: original configuration remains", ex);
        } finally {
            cleanup();
        }
    }

    private void prepareNewConfig() throws Exception {
        logger.debug("Parsing new configuration...");
        String source = config.getUpdate();
        if (!config.getEnv().isEmpty()) {
            source = EnvSupport.apply(source, config.getEnv());
            logger.info("Environment variables applied to new config");
        }

        // read structure first to validate correctness!
        updateStructure = StructureReader.read(new StringReader(source));
        updateTree = CommentsReader.read(source);
        try {
            // validate comments parser correctness using snakeyaml result
            CommentsParserValidator.validate(updateTree, updateStructure);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse new configuration", ex);
        }
        logger.info("New configuration parsed");
    }

    private void prepareCurrentConfig() throws Exception {
        final File currentCfg = config.getCurrent();
        if (currentCfg.exists()) {
            logger.debug("Parsing current configuration file ({})...", currentCfg.getAbsolutePath());
            // read current file with two parsers (snake first to make sure file is valid)
            currentStructure = StructureReader.read(currentCfg);
            currentTree = CommentsReader.read(currentCfg);

            try {
                // validate comments parser correctness using snakeyaml result
                CommentsParserValidator.validate(currentTree, currentStructure);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse current config: "
                        + currentCfg.getAbsolutePath(), ex);
            }

            // removing props
            for (String prop : config.getDeleteProps()) {
                final CmtNode node = currentTree.find(prop);
                if (node != null) {
                    logger.info("Removing configuration property: {}", prop);
                    // for root level property, it would not point to tree object
                    final TreeNode<CmtNode> root = node.getRoot() == null ? currentTree : node.getRoot();
                    root.getChildren().remove(node);

                    // remove in both trees because struct tree is used for result validation
                    final StructNode str = currentStructure.find(prop);
                    // could be commented node in comments tree, not visible in struct tree
                    if (str != null) {
                        final TreeNode<StructNode> rootStr = str.getRoot() == null ? currentStructure : str.getRoot();
                        rootStr.getChildren().remove(str);
                    }
                }
                // trees are equal (validated) so can't have different branches
            }
            logger.info("New configuration parsed ({})", currentCfg.getAbsolutePath());
        } else {
            logger.info("Current configuration doesn't exist: {}", currentCfg.getAbsolutePath());
        }

        // tmp file used to catch possible writing errors and only then override old file
        work = File.createTempFile("merge-result", ".yml");
    }

    private void merge() {
        if (currentTree == null) {
            logger.debug("No need for merge: copying new configuration");
            // just copy new
            currentTree = updateTree;
        } else {
            logger.debug("Merging configurations...");
            // merge
            TreeMerger.merge(currentTree, updateTree);
            logger.info("Configuration merged");
        }
        // write merged result
        CommentsWriter.write(currentTree, work);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private void validateResult() {
        logger.debug("Validating merged result");
        try {
            // make sure updated file is valid
            final StructTree updated = StructureReader.read(work);
            if (config.isValidateResult()) {
                UpdateResultValidator.validate(updated, currentStructure, updateStructure);
                logger.info("Merged configuration correctness validated");
            } else {
                logger.warn("Result validation skipped");
            }
        } catch (Exception ex) {
            String yamlContent;
            try {
                final StringBuilder res = new StringBuilder();
                int i = 1;
                final List<String> lines = Files.readAllLines(work.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    res.append(String.format("%4s| ", i++)).append(line);
                    if (i <= lines.size()) {
                        res.append('\n');
                    }
                }
                yamlContent = res.toString();
            } catch (Exception e) {
                logger.warn("Failed to read merged file: can't show it in log", e);
                yamlContent = "\t\t<error reading merged file>";
            }
            throw new IllegalStateException("Failed to validate merge result: \n\n" + yamlContent + "\n", ex);
        }
    }

    private void backupAndReplace() throws IOException {
        // on first installation no need to backup
        final File current = config.getCurrent();
        if (config.isBackup() && current.exists()) {
            final Path backup = Paths.get(current.getAbsolutePath()
                    + "." + new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(new Date()));
            Files.copy(current.toPath(), backup);
            logger.info("Backup created: {}", backup);
        }

        Files.copy(work.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Configuration updated: {}", current.getAbsolutePath());
    }

    private void cleanup() {
        if (work != null && work.exists()) {
            try {
                Files.delete(work.toPath());
            } catch (IOException e) {
                logger.warn("Failed to cleanup", e);
            }
        }
    }
}
