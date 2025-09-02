package ru.vyarus.yaml.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.updater.parse.comments.CommentsReader;
import ru.vyarus.yaml.updater.parse.comments.CommentsWriter;
import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;
import ru.vyarus.yaml.updater.parse.comments.model.CmtTree;
import ru.vyarus.yaml.updater.parse.common.YamlModelUtils;
import ru.vyarus.yaml.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.updater.parse.struct.StructureReader;
import ru.vyarus.yaml.updater.parse.struct.model.StructNode;
import ru.vyarus.yaml.updater.parse.struct.model.StructTree;
import ru.vyarus.yaml.updater.profile.ProdConfigurator;
import ru.vyarus.yaml.updater.profile.TestConfigurator;
import ru.vyarus.yaml.updater.report.UpdateReport;
import ru.vyarus.yaml.updater.update.CommentsParserValidator;
import ru.vyarus.yaml.updater.update.EnvSupport;
import ru.vyarus.yaml.updater.update.TreeMerger;
import ru.vyarus.yaml.updater.update.UpdateResultValidator;
import ru.vyarus.yaml.updater.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
 * - Possible whitespace between property name and colon is removed
 * - Property style is taken from new file (e.g. if property name was quoted and in target file not quoted then
 * merged file would contain not quoted property)
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

    private final UpdateReport report;

    public YamlUpdater(final UpdateConfig config) {
        this.config = config;
        this.report = new UpdateReport(config.getCurrent());
    }

    /**
     * Shortcut for {@link #create(File, InputStream)}.
     *
     * @param current config file to be updated
     * @param update  update file
     * @return builder instance for chained calls
     */
    public static ProdConfigurator create(final File current, final File update) {
        try {
            return create(current, update != null ? Files.newInputStream(update.toPath()) : null);
        } catch (Exception e) {
            throw new IllegalStateException("Error updating from file '"
                    + (update != null ? update.getAbsolutePath() : "unknown") + "'", e);
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
     * @see #create(File, File) shortcut for direct file case (most common)
     * @see ru.vyarus.yaml.updater.util.FileUtils#findExistingFile(String) for loading file from classpath or url
     */
    public static ProdConfigurator create(
            final File current, final InputStream update) {
        return new ProdConfigurator(current, update);
    }

    /**
     * Special factory for testing file migrations (WITHOUT actual modifications). Essentially, it is pre-configured
     * {@link ru.vyarus.yaml.updater.UpdateConfig.Configurator#dryRun(boolean)}, but with additional reporting options.
     * <p>
     * In contrast to main factory, this one directly support loading files from fs, classpath or URL. In most cases,
     * it is assumed to be used for testing configuration migration in unit tests (assuming both old and new configs
     * are placed in classpath). It would implicitly create tmp file and copy provided config there to match main
     * contract (tmp file would be mostly hidden in the final report).
     * <p>
     * Note that in dry run, merged file content is stored as
     * {@link ru.vyarus.yaml.updater.report.UpdateReport#getDryRunResult()} and so could be manually introspected.
     *
     * @param config updating config path (fs, classpath or url)
     * @param target target config path (fs, classpath or url)
     * @return test configurator instance for chained calls
     */
    public static TestConfigurator createTest(final String config, final String target) {
        return new TestConfigurator(config, target);
    }

    /**
     * Perform configuration migration.
     *
     * @return update report object with migration details
     * @see ru.vyarus.yaml.updater.report.ReportPrinter for default report formatter
     */
    public UpdateReport execute() {
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
        return report;
    }

    private void prepareNewConfig() throws Exception {
        logger.debug("Parsing new configuration...");
        String source = config.getUpdate();
        if (!config.getEnv().isEmpty()) {
            final EnvSupport envSupport = new EnvSupport(config.getEnv());
            source = envSupport.apply(source);
            logger.info("Environment variables applied to new config");
            report.getAppliedVariables().putAll(envSupport.getApplied());
        }
        // size after variables applied
        report.setUpdateSize(source.getBytes(StandardCharsets.UTF_8).length);

        try {
            // read structure first to validate correctness!
            updateStructure = StructureReader.read(source);
            updateTree = CommentsReader.read(source);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse update config file", ex);
        }
        try {
            // validate comments parser correctness using snakeyaml result
            CommentsParserValidator.validate(updateTree, updateStructure);
        } catch (Exception ex) {
            throw new IllegalStateException("Model validation fail: comments parser tree does not match snakeyaml's "
                    + "parse tree for update config", ex);
        }
        report.setUpdateLines(updateTree.getLinesCnt());

        logger.info("New configuration parsed ({} bytes, {} lines)",
                report.getUpdateSize(), report.getUpdateLines());
        config.getListener().updateConfigParsed(updateTree, updateStructure);
    }

    private void prepareCurrentConfig() throws Exception {
        final File currentCfg = config.getCurrent();
        if (currentCfg.exists()) {
            logger.debug("Parsing current configuration file ({})...", currentCfg.getAbsolutePath());
            report.setBeforeSize(currentCfg.length());
            try {
                // read current file with two parsers (snake first to make sure file is valid)
                currentStructure = StructureReader.read(currentCfg);
                currentTree = CommentsReader.read(currentCfg);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse current config file", ex);
            }
            try {
                // validate comments parser correctness using snakeyaml result
                CommentsParserValidator.validate(currentTree, currentStructure);
            } catch (Exception ex) {
                throw new IllegalStateException("Model validation fail: comments parser tree does not match "
                        + "snakeyaml's parse tree for current config: " + currentCfg.getAbsolutePath(), ex);
            }
            report.setBeforeLinesCnt(currentTree.getLinesCnt());

            removeProperties();
            logger.info("Current configuration parsed ({} bytes, {} lines)",
                    report.getBeforeSize(), report.getBeforeLinesCnt());
            config.getListener().currentConfigParsed(updateTree, updateStructure);
        } else {
            logger.info("Current configuration doesn't exist: {}", currentCfg.getAbsolutePath());
        }

        // tmp file used to catch possible writing errors and only then override old file
        work = File.createTempFile("merge-result", ".yml");
    }

    private void removeProperties() {
        // removing props
        for (String path : config.getDeleteProps()) {
            // unescape properties in path (keys are always unescaped - without unescape it would never find
            // quoted property)
            String prop = YamlModelUtils.cleanPropertyPath(path, false);
            boolean done = removeProperty(prop);
            if (!done) {
                // it would be a very common error to use dot as a separator, so trying to replace dots
                // with real separator and try again (it's highly unlikely to have collisions)
                prop = YamlModelUtils.cleanPropertyPath(path, true);
                done = removeProperty(prop);
            }
            if (!done) {
                logger.warn("Path '{}' not removed: not found", path);
            }
        }
    }

    private boolean removeProperty(final String prop) {
        final CmtNode node = currentTree.find(prop);
        if (node != null) {
            logger.info("Removing configuration property: {}", prop);
            // register it before actual remove for proper index rendering in report
            report.addRemoved(node);
            // for root level property, it would not point to tree object
            final TreeNode<CmtNode> root = node.getRoot() == null ? currentTree : node.getRoot();

            // remove in both trees because struct tree is used for result validation
            // (trees are equal (validated) so can't have different branches)
            final StructNode str = currentStructure.find(prop);
            final TreeNode<StructNode> rootStr = str.getRoot() == null ? currentStructure : str.getRoot();
            rootStr.getChildren().remove(str);

            if (node.getRoot() != null && root.getChildren().size() == 1) {
                // if we removed all children then removing root property to avoid logic mistakes treating
                // it as normal property and not as container (detected by children presence)
                final String yamlPath = root.getYamlPath();
                logger.warn("Container property '{}' remains empty after '{}' removal - removing it to "
                        + "not mistakenly treat it as simple property", yamlPath, prop);
                // cleanup inner properties removes from report
                report.getRemoved().removeIf(pair -> pair.getPath().startsWith(yamlPath));
                removeProperty(yamlPath);
            } else {
                // remove node from root only when top node not removed itself because otherwise
                // it would be impossible to show in report list item with property on the same line
                root.getChildren().remove(node);
            }
        }
        return node != null;
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
            reportAddedNodes(currentTree);
        }
        config.getListener().merged(currentTree);
        // write merged result
        CommentsWriter.write(currentTree, work);
    }

    private void reportAddedNodes(final TreeNode<CmtNode> root) {
        for (CmtNode node : root.getChildren()) {
            // searching first added node (could be added value or added subtree)
            if (node.isAddedNode() && !node.isCommentOnly()) { // do not show trailing comment addition
                report.addAdded(node);
            } else if (node.hasChildren()) {
                // search deeper
                reportAddedNodes(node);
            }
        }
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private void validateResult() {
        logger.debug("Validating merged result");
        try {
            // make sure updated file is valid
            final StructTree updated = StructureReader.read(work);
            // if not initial copying (current tree can't be used here as it's already replaced by new config)
            if (currentStructure != null) {
                if (config.isValidateResult()) {
                    UpdateResultValidator.validate(updated, currentStructure, updateStructure);
                    logger.info("Merged file correctness validated");
                    config.getListener().validated(updated);
                } else {
                    logger.warn("Result validation skipped");
                }
            }

            report.setAfterSize(work.length());
            report.setAfterLinesCnt(Files.readAllLines(work.toPath()).size());
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
        final boolean configChanged = isConfigChanged();
        report.setConfigChanged(configChanged);
        if (config.isDryRun()) {
            report.setDryRun(true);
            // store entire merged file content for manual validation (in tests) because it disappears otherwise
            report.setDryRunResult(FileUtils.read(Files.newInputStream(work.toPath())));
            logger.warn("DRY RUN: no modifications performed (changes detected: {})", configChanged);
            return;
        }
        final File current = config.getCurrent().getAbsoluteFile();
        if (configChanged) {
            // on first installation no need to backup
            if (config.isBackup() && current.exists()) {
                final File dir = config.getBackupDir() == null ? current.getParentFile() : config.getBackupDir();
                if (!dir.exists()) {
                    // create backup path, if required
                    Files.createDirectories(dir.toPath());
                }
                final File backup = new File(dir, current.getName()
                        + "." + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
                Files.copy(current.toPath(), backup.toPath());
                logger.info("Backup created: {}", backup.getAbsolutePath());
                config.getListener().backupCreated(backup);
                report.setBackup(backup);
            }

            if (!current.exists()) {
                // create parent directories, if required
                Files.createDirectories(current.getParentFile().toPath());
            }
            Files.copy(work.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Configuration updated: {}", current.getAbsolutePath());
        } else {
            logger.info("Configuration not changed: {}", current.getAbsolutePath());
        }
    }

    private boolean isConfigChanged() throws IOException {
        boolean res = true;
        final File current = config.getCurrent();
        if (current.exists()) {
            // validate if file changed (to avoid redundant backups)
            // use trim to avoid empty line difference (could appear at the end after read-write)
            final char[] cur = String.join("\n", Files.readAllLines(current.toPath())).trim().toCharArray();
            final char[] wrk = String.join("\n", Files.readAllLines(work.toPath())).trim().toCharArray();
            if (cur.length == wrk.length) {
                res = false;
                for (int i = 0; i < cur.length; i++) {
                    if (cur[i] != wrk[i]) {
                        res = true;
                        break;
                    }
                }
            }
        }
        return res;
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
