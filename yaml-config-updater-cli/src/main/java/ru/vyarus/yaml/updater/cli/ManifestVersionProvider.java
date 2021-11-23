package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Reads version from the bundled "META-INF/VERSION" file. Manifest file can't be used because its being overridden
 * during native image build.
 *
 * @author Vyacheslav Rusakov
 * @since 04.08.2021
 */
public class ManifestVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        final String[] res;
        final InputStream in = CommandLine.class.getClassLoader().getResourceAsStream("META-INF/VERSION");
        if (in != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String version = reader.lines().collect(Collectors.joining("\n"));
                if (version.isEmpty()) {
                    version = "unknown version";
                }
                res = new String[]{version};
            }
        } else {
            res = new String[]{"version file not found"};
        }
        return res;
    }
}
