@file:JvmName("UncompressedPlugin")
package com.matyrobbrt.reposiliteplugins.uncompressed

import com.reposilite.configuration.local.LocalConfiguration
import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.web.api.RoutingSetupEvent

@Plugin(name = "uncompressed", dependencies = ["maven", "generalplugins"])
class UncompressedPlugin : ReposilitePlugin() {
    override fun initialize(): Facade? {
        extensions().registerEvent(
            RoutingSetupEvent::class.java
        ) { event: RoutingSetupEvent ->
            event.registerRoutes(
                UncompressedFacade(
                    extensions().facade(MavenFacade::class.java),
                    extensions().facade(LocalConfiguration::class.java).compressionStrategy,
                    extensions().facade(FrontendFacade::class.java)
                )
            )
        }
        return null
    }
}