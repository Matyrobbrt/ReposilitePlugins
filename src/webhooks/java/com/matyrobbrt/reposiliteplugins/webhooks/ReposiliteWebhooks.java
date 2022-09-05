package com.matyrobbrt.reposiliteplugins.webhooks;

import com.matyrobbrt.reposiliteplugins.ConfigurationProvider;
import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Plugin(
    name = "webhooks",
    dependencies = { "maven", "generalplugins" },
    settings = WebhookSettings.class
)
public class ReposiliteWebhooks extends ReposilitePlugin {
    @Override
    public @Nullable Facade initialize() {
        final var facade = new WebhookFacade(extensions().facade(MavenFacade.class), getLogger(), ConfigurationProvider.forFacade(
                WebhookSettings.class, extensions()
        ));
        extensions().registerEvent(DeployEvent.class, event -> facade.sendWebhook(event, () -> facade.getWebhookTargets(
                event.getRepository().getName(),
                event.getGav()
        )));
        return facade;
    }
}
