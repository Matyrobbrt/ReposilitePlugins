package com.matyrobbrt.reposiliteplugins;

import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

@Plugin(name = "generalplugins", settings = GeneralSettings.class, dependencies = "shared-configuration")
public class GeneralPluginsPlugin extends ReposilitePlugin {
    @Override
    public @Nullable Facade initialize() {
        return null;
    }
}
