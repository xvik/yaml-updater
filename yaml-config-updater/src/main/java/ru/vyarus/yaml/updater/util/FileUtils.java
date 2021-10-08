package ru.vyarus.yaml.updater.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Vyacheslav Rusakov
 * @since 06.10.2021
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Searches file in multiple locations.
     * <ul>
     *     <li>First checks if provided path is fs file path (relative or absolute)</li>
     *     <li>If path contains ':' then tries to load it as URL</li>
     *     <li>Otherwise, tries to load file from classpath</li>
     * </ul>
     *
     * @param path fs file path, classpath or file url
     * @return file's input stream or null if not found
     * @see #findExistingFile(String)
     */
    public static InputStream findFile(final String path) {
        InputStream res = null;
        // first check direct file
        final File file = new File(path);
        if (file.exists()) {
            try {
                res = Files.newInputStream(file.toPath());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read file: " + file.getAbsolutePath(), e);
            }
        } else if (path.indexOf(':') > 0) {
            // url
            try {
                res = new URL(path).openStream();
            } catch (FileNotFoundException | MalformedURLException ignored) {
                // malformed url - not an url then, try classpath
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load file from url: " + path, e);
            }
        }
        if (res == null) {
            // try to resolve in classpath
            res = FileUtils.class.getResourceAsStream(path);
        }
        return res;
    }

    /**
     * Same as {@link #findFile(String)}, but fails if target file not found.
     *
     * @param path fs file path, classpath or file url
     * @return file's input stream
     */
    public static InputStream findExistingFile(final String path) {
        final InputStream out = findFile(path);
        if (out == null) {
            throw new IllegalArgumentException("File not found: " + path);
        }
        return out;
    }

    /**
     * Reads file content from stream. Closes stream after read.
     *
     * @param in file stream
     * @return file content string
     */
    @SuppressWarnings("PMD.UseTryWithResources")
    public static String read(final InputStream in) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stream content", e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    /**
     * Creates temporary file by copying content of the specified file. This is required when file is loaded from
     * classpath or url in tests because updater accepts only exact file as updatable configuration.
     *
     * @param path fs file path, classpath or file url
     * @return created tmp file with content from file in specified path
     * @throws java.lang.IllegalArgumentException if target file not found
     */
    public static File copyToTempFile(final String path) {
        final InputStream content = findExistingFile(path);
        try {
            final Path res = Files.createTempFile("config", ".yml");
            Files.copy(content, res, StandardCopyOption.REPLACE_EXISTING);
            return res.toFile();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create temp file", ex);
        }
    }

    /**
     * Loads properties file from fs, classpath or url (see {@link #findFile(String)}) as map of values.
     *
     * @param path fs file path, classpath or file url
     * @return loader properties map or empty map if file not found
     */
    public static Map<String, String> loadProperties(final String path) {
        final Map<String, String> out = new HashMap<>();
        loadProperties(path, out);
        return out;
    }

    /**
     * Loads properties file from fs, classpath or url (see {@link #findFile(String)}) and appends everything
     * to provided map.
     *
     * @param path fs file path, classpath or file url
     * @param out  output map
     * @return true if file was found, false otherwise
     */
    public static boolean loadProperties(final String path, final Map<String, String> out) {
        final InputStream in = findFile(path);
        if (in != null) {
            try {
                loadProperties(in, out);
                return true;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to read variables from: " + path, ex);
            }
        }
        return false;
    }

    /**
     * Loads properties file from stream.
     *
     * @param in  input stream
     * @param out map to put properties into
     * @throws Exception on read errors
     */
    public static void loadProperties(final InputStream in, final Map<String, String> out) throws Exception {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            final Properties props = new Properties();
            props.load(reader);

            for (Object key : props.keySet()) {
                final String name = String.valueOf(key);
                final String value = props.getProperty(name);
                out.put(name, value == null ? "" : value);
            }
        }
    }
}
