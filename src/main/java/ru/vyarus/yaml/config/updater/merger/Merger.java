package ru.vyarus.yaml.config.updater.merger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.yaml.config.updater.parse.comments.model.YamlTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Merger {
    private final Logger logger = LoggerFactory.getLogger(Merger.class);

    private final MergerConfig config;
    // copy of current file to work on
    private File work;
    private YamlTree currentStructure;
    private Map<String, Object> currentYaml;

    // new file with processed variables
    private File update;
    private YamlTree updateStructure;
    private Map<String, Object> updateYaml;

    public Merger(MergerConfig config) {
        this.config = config;
    }

    public void execute() {
        try {
            backup();
            prepareCurrentConfig();
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

    private void backup() throws IOException {
        // on first installation no need to backup
        final File current = config.getCurrent();
        if (config.isBackup() && current.exists()) {
            final Path backup = Paths.get(current.getAbsolutePath()
                    + "." + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()));
            Files.copy(current.toPath(), backup);
            logger.info("Backup created: {}", backup);
        }
    }

    private void prepareCurrentConfig() throws Exception {

    }

    private void prepareNewConfig() {

    }

    private void merge() {

    }

    private void validateResult() {

    }

    private void cleanup() {
        if (work.exists()) {
            try {
                Files.delete(work.toPath());
            } catch (IOException e) {
                logger.warn("Failed to cleanup", e);
            }
        }
        if (update.exists()) {
            try {
                Files.delete(update.toPath());
            } catch (IOException e) {
                logger.warn("Failed to cleanup", e);
            }
        }
    }

    private void rollback() {

    }
}
