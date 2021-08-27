package ru.vyarus.yaml.updater.dropwizard

import com.github.stefanbirkner.systemlambda.SystemLambda
import ru.vyarus.yaml.updater.dropwizard.support.SampleApp
import spock.lang.Specification
import spock.lang.TempDir

/**
 * @author Vyacheslav Rusakov
 * @since 23.07.2021
 */
class AbstractTest extends Specification {

    @TempDir File root

    protected String run(String scrFile, String updFile, String... args) {
        // important to update config in temp directory (otherwise it will override sample file)
        def cfg = new File(root, "config.yml")

        def text = new File(scrFile).text
        cfg.text = text

        def arg = ["update-config", cfg.absolutePath, updFile]
        arg.addAll(args)
        println "Args: $arg";
        new SampleApp().run(arg as String[])

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
        return runSpecial(false, args)
    }

    protected String runWithError(String... args) {
        return runSpecial(true, args)
    }

    private String runSpecial(boolean returnErrors, String... args) {
        PrintStream sysOut = System.out
        PrintStream sysErr = System.err

        ByteArrayOutputStream sw = new ByteArrayOutputStream()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        try {
            def arg = ["update-config"]
            arg.addAll(args)
            println "Args: $arg"

            System.setOut(new PrintStream(sw))
            System.setErr(new PrintStream(err))

            SystemLambda.catchSystemExit({
                new SampleApp().run(arg as String[])
            })
            return returnErrors ? err.toString(): sw.toString()
        } finally {
            System.setOut(sysOut)
            System.setErr(sysErr)

            println  sw.toString()
            println  err.toString()
        }
    }
}
