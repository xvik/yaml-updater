package ru.vyarus.yaml.updater.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Vyacheslav Rusakov
 * @since 20.07.2021
 */
@Command(name = "update-config", mixinStandardHelpOptions = true,
        description = "Update yaml configuration file from new file")
public class UpdateConfig implements Callable<Void> {

    @Parameters(index = "0", paramLabel = "CONFIG", description = "Path to updating configuration file")
    File current;

    @Parameters(index = "1", paramLabel = "UPDATE", description = "Path to new configuration file")
    File update;

    @Option(names = {"-b", "--no-backup"}, paramLabel = "BACKUP",
            description = "Don't create backup before configuration update")
    boolean backup;

    @Option(names = {"-d", "--delete-paths"}, arity = "1..*", paramLabel = "DELETE",
            description = "Delete properties from the current config before update")
    List<String> removePaths;

    @Option(names = {"-e", "--env"}, arity = "1..*", paramLabel = "ENV",
            description = "Variables to replace (name=value) or path(s) to properties file with variables")
    List<String> env;

    @Option(names = {"-v", "--no-validate"}, paramLabel = "VAL",
            description = "Don't validate the resulted configuration")
    boolean valid;

    @Option(names = {"-i", "--verbose"}, paramLabel = "VERBOSE",
            description = "Show debug logs")
    boolean verbose;

    @Spec
    CommandSpec spec;

    @Override
    public Void call() throws Exception {
        return null;
    }

    public static void main(final String[] args) {
        new CommandLine(new UpdateConfig()).execute(args);
    }
}
