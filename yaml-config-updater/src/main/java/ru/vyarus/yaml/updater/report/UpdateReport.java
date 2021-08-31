package ru.vyarus.yaml.updater.report;

import ru.vyarus.yaml.updater.parse.comments.model.CmtNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Update report.
 *
 * @author Vyacheslav Rusakov
 * @since 31.08.2021
 */
public class UpdateReport {

    private final File config;

    private long beforeSize;
    private int beforeLinesCnt;
    private long afterSize;
    private int afterLinesCnt;

    // NOTE it would be TRIMMED size and after variables replacement
    private long updateSize;
    private int updateLines;

    private final Map<String, String> appliedVariables = new HashMap<>();
    private final List<Pair> removed = new ArrayList<>();
    private final List<Pair> added = new ArrayList<>();

    // config might not change after update and so file not touched (no backup)
    private boolean configChanged;
    private File backup;

    public UpdateReport(final File config) {
        this.config = config;
    }

    public File getConfig() {
        return config;
    }

    public long getBeforeSize() {
        return beforeSize;
    }

    public void setBeforeSize(final long beforeSize) {
        this.beforeSize = beforeSize;
    }

    public int getBeforeLinesCnt() {
        return beforeLinesCnt;
    }

    public void setBeforeLinesCnt(final int beforeLinesCnt) {
        this.beforeLinesCnt = beforeLinesCnt;
    }

    public long getAfterSize() {
        return afterSize;
    }

    public void setAfterSize(final long afterSize) {
        this.afterSize = afterSize;
    }

    public int getAfterLinesCnt() {
        return afterLinesCnt;
    }

    public void setAfterLinesCnt(final int afterLinesCnt) {
        this.afterLinesCnt = afterLinesCnt;
    }

    public long getUpdateSize() {
        return updateSize;
    }

    public void setUpdateSize(final long updateSize) {
        this.updateSize = updateSize;
    }

    public int getUpdateLines() {
        return updateLines;
    }

    public void setUpdateLines(final int updateLines) {
        this.updateLines = updateLines;
    }

    public Map<String, String> getAppliedVariables() {
        return appliedVariables;
    }

    public List<Pair> getRemoved() {
        return removed;
    }

    public List<Pair> getAdded() {
        return added;
    }

    public void addRemoved(final CmtNode node) {
        removed.add(formatPair(node));
    }

    public void addAdded(final CmtNode node) {
        added.add(formatPair(node));
    }

    public boolean isConfigChanged() {
        return configChanged;
    }

    public void setConfigChanged(final boolean configChanged) {
        this.configChanged = configChanged;
    }

    public File getBackup() {
        return backup;
    }

    public void setBackup(final File backup) {
        this.backup = backup;
    }

    private Pair formatPair(final CmtNode node) {
        return new UpdateReport.Pair(node.getYamlPath(),
                String.format("%-3s", node.getLineNum()) + "| " + node);
    }

    /**
     * Yaml path - line identity pair.
     */
    public static class Pair {
        private final String path;
        private final String value;

        public Pair(final String path, final String value) {
            this.path = path;
            this.value = value;
        }

        public String getPath() {
            return path;
        }

        public String getValue() {
            return value;
        }
    }
}
