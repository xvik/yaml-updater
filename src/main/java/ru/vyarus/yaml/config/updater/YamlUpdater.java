package ru.vyarus.yaml.config.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.config.updater.parse.comments.CommentsReader;
import ru.vyarus.yaml.config.updater.parse.comments.CommentsWriter;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.common.model.TreeNode;
import ru.vyarus.yaml.config.updater.parse.struct.StructureReader;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStruct;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree;
import ru.vyarus.yaml.config.updater.update.CommentsParserValidator;
import ru.vyarus.yaml.config.updater.update.EnvSupport;
import ru.vyarus.yaml.config.updater.update.TreeMerger;
import ru.vyarus.yaml.config.updater.update.UpdateResultValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class YamlUpdater {
    private final Logger logger = LoggerFactory.getLogger(YamlUpdater.class);

    private final UpdateConfig config;
    // merge result until final validation (tmp file)
    private File work;
    private YamlStructTree currentStructure;
    private YamlTree currentTree;

    private YamlStructTree updateStructure;
    private YamlTree updateTree;

    /**
     * Shortcut for {@link #create(File, InputStream)}.
     *
     * @param current config file to be updated
     * @param update  update file
     * @return builder instance for chained calls
     */
    public static UpdateConfig.Builder create(final File current, final File update) {
        try {
            return create(current, new FileInputStream(update));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Update file '" + update.getAbsolutePath() + "' not found", e);
        }
    }

    /**
     * Creates updater. Update file might be physical file, classpath resource, remote url or whatever else.
     *
     * @param current config file to be updated
     * @param update  update file content
     * @return builder instance for chained calls
     * @see #create(File, File) shortcut for direct file case (most common)
     */
    public static UpdateConfig.Builder create(final File current, final InputStream update) {
        return new UpdateConfig.Builder(current, update);
    }

    public YamlUpdater(UpdateConfig config) {
        this.config = config;
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
            throw new IllegalStateException("Failed to parse new config", ex);
        }
    }

    private void prepareCurrentConfig() throws Exception {
        final File currentCfg = config.getCurrent();
        if (currentCfg.exists()) {
            // read current file with two parsers (snake first to make sure file is valid)
            currentStructure = StructureReader.read(currentCfg);
            currentTree = CommentsReader.read(currentCfg);

            try {
                // validate comments parser correctness using snakeyaml result
                CommentsParserValidator.validate(currentTree, currentStructure);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse current config: "
                        + config.getCurrent().getAbsolutePath(), ex);
            }

            // removing props
            for (String prop : config.getDeleteProps()) {
                YamlNode node = currentTree.find(prop);
                if (node != null) {
                    logger.info("Removing old config property: {}", prop);
                    // for root level property, it would not point to tree object
                    TreeNode<YamlNode> root = node.getRoot() == null ? currentTree : node.getRoot();
                    root.getChildren().remove(node);

                    // remove in both trees because struct tree is used for result validation
                    YamlStruct str = currentStructure.find(prop);
                    // could be commented node in comments tree, not visible in struct tree
                    if (str != null) {
                        TreeNode<YamlStruct> rootStr = str.getRoot() == null ? currentStructure : str.getRoot();
                        rootStr.getChildren().remove(str);
                    }
                }
                // trees are equal (validated) so can't have different branches
            }
        }
        // tmp file used to catch possible writing errors and only then override old file
        work = File.createTempFile("merge-result", ".yml");
    }

    private void merge() {
        if (currentTree == null) {
            // just copy new
            currentTree = updateTree;
        } else {
            // merge
            TreeMerger.merge(currentTree, updateTree);
        }
        // write merged result
        CommentsWriter.write(currentTree, work);
    }

    private void validateResult() {
        try {
            // make sure updated file is valid
            YamlStructTree updated = StructureReader.read(work);
            if (config.isValidateResult()) {
                logger.warn("Result validation skipped");
                UpdateResultValidator.validate(updated, currentStructure, updateStructure);
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
                        res.append("\n");
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
                    + "." + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()));
            Files.copy(current.toPath(), backup);
            logger.info("Backup created: {}", backup);
        }

        Files.copy(work.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
