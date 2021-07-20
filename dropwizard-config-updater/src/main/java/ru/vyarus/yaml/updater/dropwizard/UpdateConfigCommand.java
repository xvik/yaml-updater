package ru.vyarus.yaml.updater.dropwizard;

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * @author Vyacheslav Rusakov
 * @since 17.07.2021
 */
public class UpdateConfigCommand extends Command {

    public UpdateConfigCommand() {
        super("update-config", "Update configuration file from the new file");
    }

    @Override
    public void configure(final Subparser subparser) {
        subparser.addArgument("file")
                .nargs(1)
                .required(true)
                .type(Arguments.fileType())
                .help("Path to updating configuration file");

        subparser.addArgument("update")
                .nargs(1)
                .required(true)
                .help("Path to new configuration file. Could also be a classpath path.");

        subparser.addArgument("-b", "--no-backup")
                .dest("backup")
                .action(Arguments.storeFalse())
                .setDefault(true)
                .help("Create backup before configuration update");

        subparser.addArgument("-d", "--delete-paths")
                .dest("delete")
                .nargs("+")
                .help("Delete properties from the current config before update");

        subparser.addArgument("-e", "--env")
                .dest("env")
                .type(Arguments.fileType())
                .nargs(1)
                .help("Properties file with variables for substitution in the updating file");

        subparser.addArgument("-v", "--no-validate")
                .dest("validate")
                .action(Arguments.storeFalse())
                .setDefault(true)
                .help("Validate the resulted configuration");
    }

    @Override
    public void run(final Bootstrap<?> bootstrap, final Namespace namespace) throws Exception {

    }
}
