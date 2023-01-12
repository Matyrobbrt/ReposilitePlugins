package com.matyrobbrt.reposiliteplugins.restart;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws Throwable {
        final Optional<ProcessHandle> repoProcess = ProcessHandle.of(Long.parseLong(args[0]))
                .filter(ProcessHandle::isAlive);
        if (repoProcess.isPresent()) {
            repoProcess.get().onExit().get(); // Await the launcher exit
        }

        if (!args[1].equals("none")) {
            Files.delete(Path.of(args[1]));
        }

        System.out.println("Copied plugins. Starting maven.");

        final var os = getOS();
        final var scriptPath = resolveScriptPath(os);
        final var restartCmd = resolveScript(getOS(), URLDecoder.decode(args[2], StandardCharsets.UTF_8));

        if (!os.contains("win")) { // Not windows, so we need to make the file executable
            final var process = new ProcessBuilder("chmod", "+x", scriptPath.toString())
                    .inheritIO()
                    .start();
            process.onExit().whenComplete(($, $$) -> {
                try {
                    new ProcessBuilder(restartCmd)
                            .inheritIO()
                            .start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.exit(0);
            });
        } else {
            new ProcessBuilder(restartCmd)
                    .inheritIO()
                    .start();
            System.exit(0);
        }
    }

    public static List<String> resolveScript(String os, String script) throws IOException {
        script = script.replace(findJavaBinary(), "");
        if (script.startsWith("\"\" ")) script = script.substring(3);
        final Path args = Path.of("jvmArgs.txt");
        Files.writeString(args, script);

        script = "\"" + findJavaBinary() + "\" @jvmArgs.txt";

        System.out.println("Script is " + script);
        final Path path = resolveScriptPath(os);
        final List<String> cmd;
        final String scriptFull;
        if (os.contains("win")) {
            scriptFull = "cmd /c " + script + " %*";
            cmd = List.of(path.toString());
        } else {
            scriptFull = "#!/usr/bin/env sh\n" + script + " " + "\"$@\"";
            cmd = List.of("sh", path.toString());
        }
        Files.writeString(path, scriptFull);
        return cmd;
    }

    public static Path resolveScriptPath(String os) {
        return os.contains("win") ? Path.of("relaunch.bat").toAbsolutePath() : Path.of("relaunch.sh").toAbsolutePath();
    }

    private static String getOS() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT);
    }

    public static String findJavaBinary() {
        return ProcessHandle.current().info().command().orElse("java");
    }
}
