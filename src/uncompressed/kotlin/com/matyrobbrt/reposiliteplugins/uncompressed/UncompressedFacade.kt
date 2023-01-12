package com.matyrobbrt.reposiliteplugins.uncompressed

import com.reposilite.frontend.FrontendFacade
import com.reposilite.maven.MavenFacade
import com.reposilite.maven.api.LookupRequest
import com.reposilite.maven.infrastructure.MavenRoutes
import com.reposilite.shared.extensions.*
import com.reposilite.web.api.ReposiliteRoute
import com.reposilite.web.routing.RouteMethod.GET
import com.reposilite.web.routing.RouteMethod.HEAD
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.openapi.*
import io.javalin.openapi.ContentType.FORM_DATA_MULTIPART
import panda.std.reactive.Reference
import java.io.InputStream
import java.net.URLEncoder

internal class UncompressedFacade(
    mavenFacade: MavenFacade,
    private val compressionStrategy: Reference<String>,
    private val frontendFacade: FrontendFacade
) : MavenRoutes(mavenFacade) {

    @OpenApi(
        path = "/uncompressed/{repository}/{gav}",
        methods = [HttpMethod.GET],
        tags = ["Maven"],
        summary = "Browse the contents of repositories",
        description = "The route may return various responses to properly handle Maven specification and frontend application using the same path.",
        pathParams = [
            OpenApiParam(name = "repository", description = "Destination repository", required = true),
            OpenApiParam(name = "gav", description = "Artifact path qualifier", required = true, allowEmptyValue = true)
        ],
        responses = [
            OpenApiResponse(status = "200", description = "Input stream of requested file", content = [OpenApiContent(type = FORM_DATA_MULTIPART)]),
            OpenApiResponse(status = "404", description = "Returns 404 (for Maven) with frontend (for user) as a response if requested resource is not located in the current repository")
        ]
    )
    private val findFile = ReposiliteRoute<Unit>("/uncompressed/{repository}/<gav>", HEAD, GET) {
        accessed {
            requireGav { gav ->
                LookupRequest(this?.identifier, requireParameter("repository"), gav)
                    .let { request -> mavenFacade.findFile(request)}
                    .peek { (details, file) ->
                        ctx.resultAttachment(details.name, details.contentType, details.contentLength, compressionStrategy.get(), false, file)
                    }
                    .onError {
                        ctx.status(it.status).html(frontendFacade.createNotFoundPage(uri, it.message))
                        mavenFacade.logger.debug("FIND | Could not find file due to $it")
                    }
            }
        }
    }

    override val routes: Set<ReposiliteRoute<Any>> = routes(findFile)

    private fun Context.resultAttachment(
        name: String,
        contentType: ContentType,
        contentLength: Long,
        compressionStrategy: String,
        compress: Boolean,
        data: InputStream
    ) {
        if (!contentType.isHumanReadable) {
            contentDisposition("""attachment; filename="$name"; filename*=utf-8''${URLEncoder.encode(name, "utf-8")}""")
        }

        if ((!compress || compressionStrategy == "none") && contentLength > 0) {
            contentLength(contentLength) // Using this with GZIP ends up with "Premature end of Content-Length delimited message body".
        }

        when {
            acceptsBody() -> when {
                compress -> result(data)
                else -> data.transferTo(res().outputStream)
            }
            else -> data.silentClose()
        }

        contentType(contentType)
    }
}