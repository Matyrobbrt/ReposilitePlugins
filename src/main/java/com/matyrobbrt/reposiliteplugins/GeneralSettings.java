package com.matyrobbrt.reposiliteplugins;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.SharedSettings;

@Doc(title = "General settings", description = "General settings")
public final class GeneralSettings implements SharedSettings {
    public String baseUrl = "https://maven.example.com";

    @Doc(title = "baseUrl", description = "The base URL of the maven repository.")
    public String getBaseUrl() {
        return baseUrl;
    }
}
