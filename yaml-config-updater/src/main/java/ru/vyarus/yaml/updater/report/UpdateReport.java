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

    private long updateSize;
    private int updateLines;

    private final Map<String, String> appliedVariables = new HashMap<>();
    private final List<Pair> removed = new ArrayList<>();
    private final List<Pair> added = new ArrayList<>();

    // config might not change after update and so file not touched (no backup)
    private boolean configChanged;
    private File backup;

    // dummy execution
    private boolean dryRun;
    // complete updated file config (populated only in dry run)
    private String dryRunResult;

    public UpdateReport(final File config) {
        this.config = config;
    }

    /**
     * @return updating configuration file
     */
    public File getConfig() {
        return config;
    }

    /**
     * NOTE: size obtained as {@link java.io.File#length()} and may be slightly different on different OS.
     *
     * @return original file size
     */
    public long getBeforeSize() {
        return beforeSize;
    }

    public void setBeforeSize(final long beforeSize) {
        this.beforeSize = beforeSize;
    }

    /**
     * @return original file lines count
     */
    public int getBeforeLinesCnt() {
        return beforeLinesCnt;
    }

    public void setBeforeLinesCnt(final int beforeLinesCnt) {
        this.beforeLinesCnt = beforeLinesCnt;
    }

    /**
     * NOTE: size obtained as {@link java.io.File#length()} and may be slightly different on different OS.
     *
     * @return merged file size
     */
    public long getAfterSize() {
        return afterSize;
    }

    public void setAfterSize(final long afterSize) {
        this.afterSize = afterSize;
    }

    /**
     * @return merged file lines count
     */
    public int getAfterLinesCnt() {
        return afterLinesCnt;
    }

    public void setAfterLinesCnt(final int afterLinesCnt) {
        this.afterLinesCnt = afterLinesCnt;
    }

    /**
     * NOTE: update file length calculated from the content instead of calling {@link java.io.File#length()} (which
     * is OS dependent). So, it could appear that even when file if merged with itself, the size would be a bit
     * different.
     *
     * @return updating file size
     */
    public long getUpdateSize() {
        return updateSize;
    }

    public void setUpdateSize(final long updateSize) {
        this.updateSize = updateSize;
    }

    /**
     * @return update file lines count
     */
    public int getUpdateLines() {
        return updateLines;
    }

    public void setUpdateLines(final int updateLines) {
        this.updateLines = updateLines;
    }

    /**
     * @return variables replaced in update file
     */
    public Map<String, String> getAppliedVariables() {
        return appliedVariables;
    }

    /**
     * @return removed properties or empty list
     */
    public List<Pair> getRemoved() {
        return removed;
    }

    /**
     * @return added properties or empty list
     */
    public List<Pair> getAdded() {
        return added;
    }

    public void addRemoved(final CmtNode node) {
        removed.add(formatPair(node));
    }

    public void addAdded(final CmtNode node) {
        added.add(formatPair(node));
    }

    /**
     * If merged file content is identical to the original file (ignoring leading/trailing whitespaces) then original
     * file is not touched.
     * <p>
     * NOTE: in dry run would correctly indicate if file is changed
     *
     * @return true if configuration file was changed
     */
    public boolean isConfigChanged() {
        return configChanged;
    }

    public void setConfigChanged(final boolean configChanged) {
        this.configChanged = configChanged;
    }

    /**
     * NOTE: in dry run always return null.
     *
     * @return created backup file or null if backup not created
     */
    public File getBackup() {
        return backup;
    }

    public void setBackup(final File backup) {
        this.backup = backup;
    }

    /**
     * Changed configuration could be obtained with {@link #getDryRunResult()} (because otherwise it is not stored
     * anywhere).
     *
     * @return true if execution without modifications performed (config will not be changed)
     */
    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * NOTE: set ONLY for dry run ({@link #isDryRun()}), in normal execution you can always read updated file content
     * (in dry run content is not saved).
     *
     * @return complete update file text
     */
    public String getDryRunResult() {
        return dryRunResult;
    }

    public void setDryRunResult(final String dryRunResult) {
        this.dryRunResult = dryRunResult;
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

        /**
         * @return property yaml path
         */
        public String getPath() {
            return path;
        }

        /**
         * @return property value representation
         */
        public String getValue() {
            return value;
        }
    }
}
