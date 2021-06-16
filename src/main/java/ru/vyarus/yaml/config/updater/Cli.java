package ru.vyarus.yaml.config.updater;

import ru.vyarus.yaml.config.updater.merger.YamlMerger;
import ru.vyarus.yaml.config.updater.merger.MergeConfig;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Cli {

    public static void main(String[] args) {
        YamlMerger.builder(new File("foo"), new File("bar"))
                .backup(false)
                .build().execute();
    }


}
