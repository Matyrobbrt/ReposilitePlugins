package com.matyrobbrt.reposiliteplugins.defaultrepository

import com.matyrobbrt.reposiliteplugins.ConfigurationProvider
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.plugin.api.Facade
import com.reposilite.plugin.api.Plugin
import com.reposilite.plugin.api.ReposilitePlugin
import com.reposilite.shared.extensions.acceptsBody
import com.reposilite.shared.extensions.contentDisposition
import com.reposilite.shared.extensions.contentLength
import com.reposilite.shared.extensions.silentClose
import com.reposilite.storage.api.Location
import com.reposilite.web.api.HttpServerInitializationEvent
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.Header.CACHE_CONTROL
import java.io.InputStream
import java.net.URLEncoder
import kotlin.time.Duration.Companion.hours

@Plugin(
    name = "default-repository",
    dependencies = ["generalplugins", "maven"],
    settings = DefaultRepositorySettings::class
)
class DefaultRepositoryPlugin : ReposilitePlugin() {
    internal val maxAge = System.getProperty("reposilite.maven.maxAge", 1.hours.inWholeSeconds.toString()).toLong()

    override fun initialize(): Facade? {
        val maven = extensions().facade(MavenFacade::class.java)
        val config = ConfigurationProvider.forFacade(
            DefaultRepositorySettings::class.java, extensions()
        )
        extensions().registerEvent(
            HttpServerInitializationEvent::class.java
        ) { event: HttpServerInitializationEvent ->
            event.javalin.before { context: Context ->
                val pathParts =
                    context.path().split("/".toRegex(), limit = 2).toTypedArray()
                if (pathParts.size == 2) {
                    if (maven.getRepository(pathParts[0]) != null) {
                        return@before
                    }
                    val defaultRepo = config.get().repository ?: return@before
                    maven.findFile(
                        LookupRequest(
                            null, defaultRepo, Location.of(context.path())
                        )
                    )
                        .peek { (details, file, cachable) ->
                            val determinedExtension = context.path().substringAfterLast(".", "")
                            context.resultAttachment(
                                details.name,
                                if (determinedExtension == "") details.contentType else (ContentType.getContentTypeByExtension(determinedExtension) ?: ContentType.APPLICATION_OCTET_STREAM),
                                details.contentLength,
                                file, cachable
                            )
                            throw BadException()
                        }
                }
            }
            event.javalin.exception(BadException::class.java) { _, _ ->
                // Just ignore
            }
        }
        return null
    }

    private fun Context.resultAttachment(
        name: String,
        contentType: ContentType,
        contentLength: Long,
        data: InputStream,
        cache: Boolean
    ) {
        if (!contentType.isHumanReadable) {
            contentDisposition("""attachment; filename="$name"; filename*=utf-8''${URLEncoder.encode(name, "utf-8")}""")
        }

        contentLength(contentLength)

        if (cache) {
            header(CACHE_CONTROL, "public, max-age=$maxAge")
        } else {
            header(CACHE_CONTROL, "no-cache, no-store, max-age=0")
        }

        if (acceptsBody()) {
            data.copyTo(res().outputStream)
        }
        data.silentClose()

        contentType(contentType)
        status(200)
    }

    class BadException : Exception() {}
}