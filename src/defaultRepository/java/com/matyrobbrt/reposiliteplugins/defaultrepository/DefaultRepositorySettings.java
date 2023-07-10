package com.matyrobbrt.reposiliteplugins.defaultrepository;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.SharedSettings;

@Doc(title = "Default Repository", description = "Default Repository settings")
public final class DefaultRepositorySettings implements SharedSettings {
    public String repository;
    @Doc(title = "repository", description = "The default repository requests should be routed to")
    public String getRepository() {
        return repository;
    }
}
