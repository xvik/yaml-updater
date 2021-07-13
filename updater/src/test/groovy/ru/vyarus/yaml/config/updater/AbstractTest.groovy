package ru.vyarus.yaml.config.updater

import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.07.2021
 */
abstract class AbstractTest extends Specification {

    protected String unifyString(String input) {
        return input
        // cleanup win line break for simpler comparisons
                .replace("\r", '')
    }
}
