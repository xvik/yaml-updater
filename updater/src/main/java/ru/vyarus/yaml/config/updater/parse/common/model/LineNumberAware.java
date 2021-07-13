package ru.vyarus.yaml.config.updater.parse.common.model;

/**
 * Unification for yaml nodes and tree node (not mapped object, grouping all root yaml properties).
 *
 * @author Vyacheslav Rusakov
 * @since 18.05.2021
 */
public interface LineNumberAware {

    /**
     * @return yaml line number, starting from 1, or 0 to indicate tree root
     */
    int getLineNum();
}
