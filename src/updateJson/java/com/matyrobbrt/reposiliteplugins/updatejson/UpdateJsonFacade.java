package com.matyrobbrt.reposiliteplugins.updatejson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.maven.api.LatestArtifactQuery;
import com.reposilite.maven.api.LatestArtifactQueryRequest;
import com.reposilite.maven.api.VersionLookupRequest;
import com.reposilite.plugin.api.Facade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

public class UpdateJsonFacade implements Facade {
    public static final String PROMO_NAME = "forge-promotions.json";
    public static final Gson GSON  = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final MavenFacade maven;

    public UpdateJsonFacade(MavenFacade maven) {
        this.maven = maven;
    }

    public void handle(DeployEvent event) throws IOException {
        if (!event.getGav().toString().endsWith("maven-metadata.xml")) return;
        final var gav = event.getGav().locationBeforeLast("/", "");
        final var latestVersion = maven.findLatestVersion(new VersionLookupRequest(
                null, event.getRepository(), gav, null
        )).orNull();
        if (latestVersion == null) return;
        final var latest = maven.findLatestVersionFile(new LatestArtifactQueryRequest(null, event.getRepository(), new LatestArtifactQuery(
                gav, "jar", null, null
        )), maven::findFile).orNull();
        if (latest == null) return;
        try (final var jarIs = new JarInputStream(latest.component2())) {
            final var manifest = jarIs.getManifest().getMainAttributes();
            if (manifest.getValue("Built-on-Minecraft") == null) return;
            final var promos = gav.resolve(PROMO_NAME);
            final var existingJson = event.getRepository().getFile(promos).mapErr(it -> new JsonObject()).map(in -> {
                try (final var reader = new InputStreamReader(in)) {
                    return GSON.fromJson(reader, JsonObject.class);
                } catch (Exception ignored) {}
                return new JsonObject();
            }).<JsonObject>getAnyAs();
            if (manifest.containsKey(new Attributes.Name("DownloadHomepage"))) {
                existingJson.addProperty("homepage", manifest.getValue("DownloadHomepage"));
            }

            final JsonObject promosObject;
            if (existingJson.has("promos")) {
                promosObject = existingJson.getAsJsonObject("promos");
            } else {
                promosObject = new JsonObject();
            }
            promosObject.addProperty(manifest.getValue("Built-on-Minecraft") + ((
                        manifest.containsKey(new Attributes.Name("RecommendedBuild"))
            ) ? "-recommended" : "-latest"), manifest.getValue(Attributes.Name.IMPLEMENTATION_VERSION));

            existingJson.add("promos", promosObject);
            event.getRepository().putFile(promos, new ByteArrayInputStream(GSON.toJson(existingJson).getBytes(StandardCharsets.UTF_8)));
        }
    }
}
