package com.matyrobbrt.reposiliteplugins.webhooks;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.Min;
import com.reposilite.configuration.shared.api.SharedSettings;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Doc(title = "Webhooks", description = "Webhook settings")
public final class WebhookSettings implements SharedSettings {

    public List<Webhook> webhooks = List.of(
            new Webhook("example", "https://example.com", List.of(
                    new Artifact("com.example", "example")
            ))
    );
    @Doc(title = "webhooks", description = "All webhooks")
    public List<Webhook> getWebhooks() {
        return webhooks;
    }

    @Doc(title = "Webhook", description = "Configuration for a specific webhook")
    public record Webhook(
            @Min(min = 1)
            @Doc(title = "id", description = "The ID (name) of the webhook")
            String id,
            @Doc(title = "url", description = "The URL of the webhook")
            String webhookUrl,
            @Doc(title = "artifacts", description = "The artifacts this webhook listens for.e")
            List<Artifact> artifacts
    ) {}
    public record Artifact(
            @Doc(title = "group", description = "The artifact's group")
            String group,
            @Min(min = 1)
            @Doc(title = "id", description = "The artifact ID")
            String id
    ) implements Predicate<String> {
        @Override
        public boolean test(String s) {
            if (id.startsWith("regex:")) {
                final var pattern = Pattern.compile(id.substring("regex:".length()));
                return pattern.matcher(s).matches();
            }
            return id.equals(s);
        }
    }
}
