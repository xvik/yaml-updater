package ru.vyarus.yaml.config.updater.merger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.config.updater.parse.comments.CommentsReader;
import ru.vyarus.yaml.config.updater.parse.comments.CommentsWriter;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.parse.struct.StructureReader;
import ru.vyarus.yaml.config.updater.parse.struct.model.YamlStructTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
public class Merger {
    private final Logger logger = LoggerFactory.getLogger(Merger.class);

    private final MergerConfig config;
    // merge result until final validation (tmp file)
    private File work;
    private YamlStructTree currentStructure;
    private YamlTree currentTree;

    private YamlStructTree updateStructure;
    private YamlTree updateTree;

    public Merger(MergerConfig config) {
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
        final File updCfg = config.getUpdate();
        File update = null;
        try {
            update = File.createTempFile("update", ".yml");
            // todo implement environment appliance
            try (final FileOutputStream out = new FileOutputStream(update)) {
                Files.copy(updCfg.toPath(), out);
            }

            // read structure first to validate correctness!
            updateStructure = StructureReader.read(update);
            updateTree = CommentsReader.read(update);
            try {
                // validate comments parser correctness using snakeyaml result
                ParserValidator.validate(updateTree, updateStructure);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse new config: "
                        + config.getUpdate().getAbsolutePath(), ex);
            }
        } finally {
            if (update != null) {
                try {
                    Files.delete(update.toPath());
                } catch (IOException e) {
                    logger.warn("Failed to cleanup", e);
                }
            }
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
                ParserValidator.validate(currentTree, currentStructure);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to parse current config: "
                        + config.getCurrent().getAbsolutePath(), ex);
            }
        }
        // tmp file used to catch possible writing errors and only then override old file
        work = File.createTempFile("merge-result", ".yml");

//        if (!config.getDeleteProps().isEmpty()) {
        // todo remove nodes
//        }
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
            ResultValidator.validate(updated, currentStructure, updateStructure);
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
