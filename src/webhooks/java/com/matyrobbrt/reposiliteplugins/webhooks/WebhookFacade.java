package com.matyrobbrt.reposiliteplugins.webhooks;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.matyrobbrt.reposiliteplugins.ConfigurationProvider;
import com.matyrobbrt.reposiliteplugins.Utils;
import com.reposilite.journalist.Logger;
import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.maven.api.VersionLookupRequest;
import com.reposilite.plugin.api.Facade;
import com.reposilite.storage.api.Location;
import okhttp3.OkHttpClient;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public record WebhookFacade(MavenFacade maven, Logger logger, ConfigurationProvider<WebhookSettings> settings) implements Facade {
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        final var thread = new Thread(r, "WebhookExecutor");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, WebhookClient> CLIENTS = new ConcurrentHashMap<>();
    private static final String ICON_URL = "https://github.com/reposilite-playground.png";

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> CLIENTS.forEach((a, b) -> b.close())));
    }

    public void sendWebhook(DeployEvent event, Supplier<List<String>> targets) {
        if (!event.getGav().toString().endsWith("maven-metadata.xml")) return;
        final var mavenUrl = settings.getGeneralSettings().baseUrl;
        var gav = event.getGav().locationBeforeLast("/", "");
        final var latestVersion = maven.findLatestVersion(new VersionLookupRequest(
                null, event.getRepository(), gav, null
        )).orNull();
        if (latestVersion == null) return;
        gav = gav.resolve(latestVersion.getVersion());
        final var baseUrl = mavenUrl + "/#/" + event.getRepository().getName() + "/";
        final var finalGav = gav;
        targets.get().forEach(target -> {
            final var client = CLIENTS.computeIfAbsent(target, $ -> new WebhookClientBuilder(target).setHttpClient(CLIENT).setExecutorService(EXECUTOR).build());
            final var embed = new WebhookEmbedBuilder();
            final var location = Objects.requireNonNull(Utils.getData(event.getRepository().getName(), finalGav));

            embed.setAuthor(new WebhookEmbed.EmbedAuthor(
                    "Reposilite", ICON_URL, mavenUrl
            ));
            embed.setColor(location.version.toLowerCase(Locale.ROOT).contains("snapshot") ? 0x080080 : 0x90ee90);
            embed.setTitle(new WebhookEmbed.EmbedTitle("A new artifact has been deployed!", baseUrl + finalGav));
            embed.setFooter(new WebhookEmbed.EmbedFooter("Deployment notifications", ICON_URL));
            embed.setTimestamp(Instant.now());

            final String by = event.getBy().substring(0, event.getBy().indexOf('@'));
            final String desc = "The version `%s` of `%s:%s` has been published by %s.\n".formatted(
                    location.version, location.group, location.name, by
            ) +
                    """
                            \n**Gradle Groovy**:
                            ```gradle
                            implementation '%s:%s:%s'
                            ```
                            """.formatted(
                            location.group, location.name, location.version
                    ) +
                    """
                            **Maven**:
                            ```xml
                            <dependency>
                                <groupId>%s</groupId>
                                <artifactId>%s</artifactId>
                                <version>%s</version>
                            </dependency>
                            ```
                            """.formatted(
                            location.group, location.name, location.version
                    );
            embed.setDescription(desc);

            client.send(new WebhookMessageBuilder()
                    .addEmbeds(embed.build())
                    .setUsername("Maven deployments")
                    .setAvatarUrl(ICON_URL)
                    .build())
                    .whenComplete(Utils.loggingErrorHandler(logger));
        });
    }

    public List<String> getWebhookTargets(String repoName, Location gav) {
        final var location = Objects.requireNonNull(Utils.getData(repoName, gav));
        return settings.get().getWebhooks().stream()
                .filter(WebhookSettings.Webhook::enabled)
                .filter(it -> it.artifacts().stream().anyMatch(art -> art.group().equals(location.group) && art.test(location.name)))
                .map(WebhookSettings.Webhook::webhookUrl)
                .toList();
    }
}
