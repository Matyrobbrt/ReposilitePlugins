package com.matyrobbrt.reposiliteplugins.updatejson;

import com.google.gson.JsonObject;
import com.matyrobbrt.reposiliteplugins.Utils;
import com.reposilite.console.CommandContext;
import com.reposilite.console.api.ReposiliteCommand;
import com.reposilite.maven.Repository;
import com.reposilite.storage.api.Location;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UpdateJsonCommands {
    public static final Logger LOG = LoggerFactory.getLogger(UpdateJsonCommands.class);
    @SuppressWarnings("DuplicatedCode")
    @CommandLine.Command(
            name = "update-json-create",
            description = "Create a Forge Update JSON url"
    )
    public static final class CreateJson implements ReposiliteCommand {
        private final RepositoryProvider repositoryService;
        public CreateJson(RepositoryProvider repositoryService) {
            this.repositoryService = repositoryService;
        }

        @CommandLine.Parameters(
                index = "0",
                paramLabel = "<path>",
                arity = "1",
                description = "The path of the artifact to create a JSON for."
        )
        public String path;
        @CommandLine.Parameters(
                index = "1",
                paramLabel = "<homepage>",
                description = "A download homepage for the JSON.",
                arity = "1",
                defaultValue = "computed"
        )
        public String homepage = "computed";

        @CommandLine.Parameters(
                index = "2",
                paramLabel = "[<repository>]",
                description = "The name of the repository",
                defaultValue = "releases"
        )
        public String repository = "releases";

        @Override
        public void execute(@NotNull CommandContext commandContext) {
            final var repo = repositoryService.find(repository);
            if (repo == null) {
                commandContext.append("Unknown repo: " + repository);
                return;
            }

            final var gav = Location.of(path.replace(':', '/').replace('.', '/'))
                    .resolve(UpdateJsonFacade.PROMO_NAME);
            final var json = new JsonObject();
            json.addProperty("homepage", homepage);
            repo.putFile(gav, new ByteArrayInputStream(Utils.GSON.toJson(json).getBytes(StandardCharsets.UTF_8)));
            commandContext.append("Updated update JSON!");
        }
    }
    @SuppressWarnings("DuplicatedCode")
    @CommandLine.Command(
            name = "update-json-set",
            description = "Set a version for the update JSON"
    )
    public static final class SetVersion implements ReposiliteCommand {
        private final RepositoryProvider repositoryService;
        public SetVersion(RepositoryProvider repositoryService) {
            this.repositoryService = repositoryService;
        }

        @CommandLine.Parameters(
                index = "0",
                paramLabel = "<path>",
                arity = "1",
                description = "The path of the artifact to update the JSON for."
        )
        public String path;
        @CommandLine.Parameters(
                index = "1",
                paramLabel = "<mcversion>",
                description = "The MCVersion to change the version for.",
                arity = "1"
        )
        public String mcVersion;
        @CommandLine.Parameters(
                index = "2",
                paramLabel = "<version>",
                description = "The version to set to.",
                arity = "1"
        )
        public String version;

        @CommandLine.Parameters(
                index = "3",
                paramLabel = "[<type>]",
                description = "The type of the version: recommended or latest",
                defaultValue = "latest"
        )
        public String type = "latest";

        @CommandLine.Parameters(
                index = "4",
                paramLabel = "[<repository>]",
                description = "The name of the repository",
                defaultValue = "releases"
        )
        public String repository = "releases";

        @Override
        public void execute(@NotNull CommandContext commandContext) {
            final var repo = repositoryService.find(repository);
            if (repo == null) {
                commandContext.append("Unknown repo: " + repository);
                return;
            }

            final var gav = Location.of(path.replace(':', '/').replace('.', '/'))
                    .resolve(UpdateJsonFacade.PROMO_NAME);
            final var is = repo.getFile(gav).orNull();
            if (is == null) {
                commandContext.append("Json promo not found!");
                return;
            }
            try (final var url = is) {
                final var json = Utils.GSON.fromJson(new InputStreamReader(url), JsonObject.class);
                final JsonObject promos;
                if (json.has("promos")) {
                    promos = json.get("promos").getAsJsonObject();
                } else {
                    promos = new JsonObject();
                }
                promos.addProperty(mcVersion + "-" + type, version);
                json.add("promos", promos);
                repo.putFile(gav, new ByteArrayInputStream(Utils.GSON.toJson(json).getBytes(StandardCharsets.UTF_8)));
                commandContext.append("Updated update JSON!");
            } catch (IOException e) {
                LOG.error("Encountered exception updating update JSON: ", e);
                commandContext.append("Exception encountered:")
                        .append(e.getMessage());
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @CommandLine.Command(
            name = "update-json-homepage",
            description = "Set a homepage for the update JSON"
    )
    public static final class SetHomepage implements ReposiliteCommand {
        private final RepositoryProvider repositoryService;
        public SetHomepage(RepositoryProvider repositoryService) {
            this.repositoryService = repositoryService;
        }

        @CommandLine.Parameters(
                index = "0",
                paramLabel = "<path>",
                arity = "1",
                description = "The path of the artifact to update the JSON for."
        )
        public String path;
        @CommandLine.Parameters(
                index = "1",
                paramLabel = "<homepage>",
                description = "The new homepage.",
                arity = "1"
        )
        public String homepage;
        @CommandLine.Parameters(
                index = "2",
                paramLabel = "[<repository>]",
                description = "The name of the repository",
                defaultValue = "releases"
        )
        public String repository = "releases";

        @Override
        public void execute(@NotNull CommandContext commandContext) {
            final var repo = repositoryService.find(repository);
            if (repo == null) {
                commandContext.append("Unknown repo: " + repository);
                return;
            }

            final var gav = Location.of(path.replace(':', '/').replace('.', '/'))
                    .resolve(UpdateJsonFacade.PROMO_NAME);
            try (final var url = repo.getFile(gav).orNull()) {
                if (url == null) {
                    commandContext.append("Json promo not found!");
                    return;
                }
                final var json = Utils.GSON.fromJson(new InputStreamReader(url), JsonObject.class);
                json.addProperty("homepage", homepage);
                repo.putFile(gav, new ByteArrayInputStream(Utils.GSON.toJson(json).getBytes(StandardCharsets.UTF_8)));
                commandContext.append("Updated update JSON!");
            } catch (IOException e) {
                LOG.error("Encountered exception updating update JSON: ", e);
                commandContext.append("Exception encountered:")
                        .append(e.getMessage());
            }
        }
    }

    public interface RepositoryProvider {
        Repository find(String name);
    }
}
