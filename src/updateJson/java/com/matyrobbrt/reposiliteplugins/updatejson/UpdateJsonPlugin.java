package com.matyrobbrt.reposiliteplugins.updatejson;

import com.reposilite.console.ConsoleFacade;
import com.reposilite.maven.MavenFacade;
import com.reposilite.maven.api.DeployEvent;
import com.reposilite.plugin.api.Facade;
import com.reposilite.plugin.api.Plugin;
import com.reposilite.plugin.api.ReposilitePlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Plugin(name = "forgeupdatejson", dependencies = "maven")
public class UpdateJsonPlugin extends ReposilitePlugin {
    @Override
    public @Nullable Facade initialize() {
        final var maven = extensions().facade(MavenFacade.class);
        var facade = new UpdateJsonFacade(maven);
        extensions().registerEvent(DeployEvent.class, event -> {
            try {
                facade.handle(event);
            } catch (IOException e) {
                getLogger().error("Exception trying to modify/create Forge UpdateJSON: {}", e);
            }
        });

        final var commands = extensions().facade(ConsoleFacade.class);
        commands.registerCommand(new UpdateJsonCommands.CreateJson(maven::getRepository));
        commands.registerCommand(new UpdateJsonCommands.SetVersion(maven::getRepository));
        commands.registerCommand(new UpdateJsonCommands.SetHomepage(maven::getRepository));

        return facade;
    }
}
