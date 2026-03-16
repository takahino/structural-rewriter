package io.github.takahino.comby;

import io.github.takahino.comby.cli.CombyCommand;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CombyCommand()).execute(args);
        System.exit(exitCode);
    }
}
