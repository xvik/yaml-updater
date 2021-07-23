package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author Vyacheslav Rusakov
 * @since 20.07.2021
 */
@Command(name = "update-config", mixinStandardHelpOptions = true,
        description = "Update yaml configuration file from new file")
public class UpdateConfig implements Callable<Void> {

    @Parameters(index = "0")
    File current;

    @Parameters(index = "1")
    String update;

    @Option(names = {"-b", "--backup"}, paramLabel = "BACKUP", description = "Backup current file before update")
    boolean backup;

    @Override
    public Void call() throws Exception {
        return null;
    }

    public static void main(final String[] args) {
        new CommandLine(new UpdateConfig()).execute(args);
    }
}
