package net.codinux.geoip.api.filter

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import net.codinux.geoip.api.PathsConfig
import net.codinux.geoip.config.GeoIpQuarkusConfig

/**
 * Admin endpoints at /api/v1/admin can be conditionally enabled or disabled e.g.
 * by setting config property `geoip.admin.enabled` or environment variable `GEOIP_ADMIN_ENABLED` to true.
 *
 * By default enabled for dev and test but not for prod.
 */
@Provider
@ApplicationScoped
class AdminEndpointsFilter(
    config: GeoIpQuarkusConfig,
) : ContainerRequestFilter {

    companion object {
        private val AdminEndpointsDisabledResponse = Response.status(Response.Status.NOT_FOUND)
            .entity("${PathsConfig.AdminBasePath} endpoints have been disabled by config. Set e.g. environment " +
                    "variable GEOIP_ADMIN_ENABLED to true to enable them.")
            .type(MediaType.TEXT_PLAIN).build()
    }


    private val adminEnabled: Boolean = config.admin().enabled()


    override fun filter(requestContext: ContainerRequestContext) {
        if (adminEnabled == false && requestContext.uriInfo.path.startsWith(PathsConfig.AdminBasePath)) {
            requestContext.abortWith(AdminEndpointsDisabledResponse)
        }
    }

}