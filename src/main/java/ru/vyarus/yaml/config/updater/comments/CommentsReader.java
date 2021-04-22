package ru.vyarus.yaml.config.updater.comments;

import ru.vyarus.yaml.config.updater.comments.model.YamlTree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class CommentsReader {

    public YamlTree read(final File yaml) {
        try {
            final List<String> lines = Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + yaml.getAbsolutePath());
        }
        YamlTree res = new YamlTree();
        return res;
    }

    private YamlTree parse(List<String> lines) {

    }
}
