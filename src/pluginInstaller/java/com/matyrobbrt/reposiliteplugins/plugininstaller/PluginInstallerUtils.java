package com.matyrobbrt.reposiliteplugins.plugininstaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class PluginInstallerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInstallerUtils.class);
    public static void restart(@Nullable Path pluginToDelete) throws Exception {
        LOGGER.info("Restarting...");

        final Path restartPath = Path.of("restarter.jar");
        try (final var is = PluginInstallerUtils.class.getResourceAsStream("/restarter.jar");
            final var os = Files.newOutputStream(restartPath)) {
            Objects.requireNonNull(is).transferTo(os);
        }

        final String cmdLine = getCommandLine(ProcessHandle.current())
                .orElseThrow(() -> new Exception("Could not determine the command to use for restarting!"));

        System.out.println("let's delete " + pluginToDelete);

        new ProcessBuilder()
                .command(findJavaBinary(), "-jar",
                        restartPath.toAbsolutePath().toString(),
                        String.valueOf(ProcessHandle.current().pid()),
                        pluginToDelete == null ? "none" : pluginToDelete.toAbsolutePath().toString(),
                        URLEncoder.encode(cmdLine, StandardCharsets.UTF_8))
                .inheritIO()
                .start();

        System.exit(0);
    }

    public static String findJavaBinary() {
        return ProcessHandle.current().info().command().orElse("java");
    }

    /**
     * Returns the full command-line of the process.
     * <p>
     * This is a workaround for
     * <a href="https://stackoverflow.com/a/46768046/14731">https://stackoverflow.com/a/46768046/14731</a>
     *
     * @param processHandle a process handle
     * @return the command-line of the process
     * @throws UncheckedIOException if an I/O error occurs
     */
    private static Optional<String> getCommandLine(ProcessHandle processHandle) throws UncheckedIOException {
        final var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return processHandle.info().commandLine();
        }
        final var desiredProcessId = processHandle.pid();
        try {
            final var process = new ProcessBuilder("wmic", "process", "where", "ProcessID=" + desiredProcessId, "get",
                    "commandline", "/format:list").
                    redirectErrorStream(true).
                    start();
            try (final var inputStreamReader = new InputStreamReader(process.getInputStream());
                 final var reader = new BufferedReader(inputStreamReader)) {
                while (true) {
                    final var line = reader.readLine();
                    if (line == null) {
                        return Optional.empty();
                    }
                    if (!line.startsWith("CommandLine=")) {
                        continue;
                    }
                    return Optional.of(line.substring("CommandLine=".length()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
