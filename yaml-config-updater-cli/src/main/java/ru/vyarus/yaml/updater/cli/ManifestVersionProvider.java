package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author Vyacheslav Rusakov
 * @since 04.08.2021
 */
public class ManifestVersionProvider implements CommandLine.IVersionProvider {

    public String[] getVersion() throws Exception {
        final URL url = CommandLine.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
        if (url != null) {
            try {
                final Manifest manifest = new Manifest(url.openStream());
                final Attributes attr = manifest.getMainAttributes();
                String version = get(attr, "Implementation-Version");
                if (version == null) {
                    version = "unknown";
                }
                return new String[]{"yaml-config-updater version " + version};
            } catch (IOException ex) {
                return new String[]{"Unable to read from " + url + ": " + ex};
            }
        }
        return new String[]{"manifest not found"};
    }

    private static String get(final Attributes attributes, final String key) {
        return (String) attributes.get(new Attributes.Name(key));
    }
}
