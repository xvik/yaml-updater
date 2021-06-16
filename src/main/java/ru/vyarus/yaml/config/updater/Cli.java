package ru.vyarus.yaml.config.updater;

import ru.vyarus.yaml.config.updater.merger.YamlMerger;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Cli {

    public static void main(String[] args) {
        YamlMerger.create(new File("foo"), new File("bar"))
                .backup(false)
                .build().execute();
    }


}
