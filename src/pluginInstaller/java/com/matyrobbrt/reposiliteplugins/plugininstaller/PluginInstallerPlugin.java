package com.matyrobbrt.reposiliteplugins.plugininstaller;

import com.reposilite.console.ConsoleFacade;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

@Plugin(name = "plugin-installer", dependencies = "generalplugins")
public class PluginInstallerPlugin extends ReposilitePlugin {
    @Override
    public @Nullable Facade initialize() {
        extensions().facade(ConsoleFacade.class).registerCommand(new InstallPluginCommand());
        return null;
    }
}
