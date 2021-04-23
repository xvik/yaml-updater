package ru.vyarus.yaml.config.updater.comments;

import ru.vyarus.yaml.config.updater.comments.model.YamlNode;
import ru.vyarus.yaml.config.updater.comments.model.YamlTree;
import ru.vyarus.yaml.config.updater.comments.util.CountingIterator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 22.04.2021
 */
public class CommentsReader {

    public YamlTree read(final File yaml) {
        try {
            final List<String> lines = Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8);
            final List<YamlNode> nodes = readNodes(new CountingIterator<>(lines.iterator()), "");
            return new YamlTree(nodes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + yaml.getAbsolutePath());
        }
    }

    private List<YamlNode> readNodes(final CountingIterator<String> lines, final String prefix) {
        final List<YamlNode> res = new ArrayList<>();
        while (lines.hasNext()) {
            final String line = lines.next();
            try {
                processLine(res, line, prefix);
            } catch (Exception ex) {
                throw new IllegalStateException("Error parsing line " + lines.getPosition(), ex);
            }
        }
        return res;
    }

    private void processLine(final List<YamlNode> parsed, final String line, final String prefix) {
        final CharacterIterator chars = new StringCharacterIterator(line);
        try {

        } catch (Exception ex) {
            throw new IllegalStateException("Error parsing line on position " + (chars.getIndex() + 1) + ": "
                    + visualizeError(line, chars), ex);
        }
    }

    private String visualizeError(final String line, final CharacterIterator chars) {
        String demo = "\n\t" + line + "\n\t";
        final int index = chars.getIndex();
        if (index > 1) {
            char[] array = new char[index - 1];
            Arrays.fill(array, '-');
            demo += new String(array);
        }
        demo += '^';
        return demo;
    }
}
