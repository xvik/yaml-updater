package ru.vyarus.yaml.config.updater;

import ru.vyarus.yaml.config.updater.merger.Merger;
import ru.vyarus.yaml.config.updater.merger.MergerConfig;

import java.io.File;

/**
 * @author Vyacheslav Rusakov
 * @since 14.04.2021
 */
public class Updater {

    public static void main(String[] args) {
        new Merger(MergerConfig
                .builder(new File("foo"), new File("bar"))
                .backup(false)
                .build()).execute();
    }


}
