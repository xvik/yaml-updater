package ru.vyarus.yaml.updater.cli

import picocli.CommandLine
import spock.lang.Specification
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 23.07.2021
 */
class AbstractTest extends Specification {

    @TempDir
    File root

    protected String run(String scrFile, String updFile, String... args) {
        // important to update config in temp directory (otherwise it will override sample file)
        def cfg = new File(root, "config.yml")

        def text = new File(scrFile).text
        cfg.text = text

        def arg = [UpdateConfigCli.CMD_NAME, cfg.absolutePath, updFile]
        arg.addAll(args)
        println "Args: $arg";
        UpdateConfigCli.main(arg as String[])

        def res = unifyString(cfg.text)
        println "RESULT:  \n-----------------------\n$res\n---------------------------\n"
        return res
    }

    protected boolean isBackupCreated() {
        root.listFiles().size() == 2
    }

    protected String unifyString(String input) {
        return input
        // cleanup win line break for simpler comparisons
                .replace("\r", '')
    }

    protected String runWithOutput(String... args) {
        CommandLine cmd = new CommandLine(new UpdateConfigCli())
        StringWriter sw = new StringWriter();
        StringWriter err = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        cmd.setErr(new PrintWriter(err))
        cmd.execute(args)
        def out = sw.toString()
        println out
        println err.toString()

        return out
    }

    protected String runWithError(String... args) {
        CommandLine cmd = new CommandLine(new UpdateConfigCli())
        StringWriter sw = new StringWriter();
        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out))
        cmd.setErr(new PrintWriter(sw));
        cmd.execute(args)
        def err = sw.toString()
        println err
        println out
        return err
    }
}
