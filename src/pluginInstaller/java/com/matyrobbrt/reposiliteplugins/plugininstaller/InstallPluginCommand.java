package com.matyrobbrt.reposiliteplugins.plugininstaller;

import com.reposilite.console.CommandContext;
import com.reposilite.console.api.ReposiliteCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@CommandLine.Command(
        name = "install-plugin",
        description = "Installs or updates a plugin."
)
public class InstallPluginCommand implements ReposiliteCommand {
    public static final Logger LOG = LoggerFactory.getLogger(InstallPluginCommand.class);

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "<url>",
            arity = "1",
            description = "The URL to download the plugin from."
    )
    public String path;

    @Override
    public void execute(@NotNull CommandContext commandContext) {
        final String[] jarNames = path.split("/");
        final String jarName = jarNames[jarNames.length - 1];

        Path targetPath = Path.of("plugins/" + jarName);
        if (Files.exists(targetPath)) {
            targetPath = Path.of("plugins/" + jarName.replace(".jar", "") + "-new.jar");
        }

        try {
            final Path toDelete;
            try (final Stream<Path> plugins = Files.list(Path.of("plugins"))) {
                toDelete = plugins.filter(it -> jarName.startsWith(it.getFileName().toString().split("-")[0])).findFirst().orElse(null);
            }

            try (final InputStream is = URI.create(path).toURL().openStream();
                 final OutputStream os = Files.newOutputStream(targetPath)) {
                is.transferTo(os);

                commandContext.append("Plugin downloaded... Restarting.");
                PluginInstallerUtils.restart(toDelete);
            }
        } catch (Throwable exception) {
            commandContext.append("Encountered exception: " + exception);
            LOG.error("Encountered exception downloading plugin from {}:", path, exception);
        }
    }
}
