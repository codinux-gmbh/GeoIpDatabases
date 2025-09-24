package net.codinux.geoip.api.filter

import io.quarkus.smallrye.openapi.OpenApiFilter
import net.codinux.geoip.api.TagsConfig
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.OpenAPI

@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
class GeoIpSchemaOASFilter : OASFilter {

    companion object {
        private val order = mapOf(
            TagsConfig.GeoIpResourceTagName to 1,
            TagsConfig.StatusPageResourceTagName to 50,
            TagsConfig.StatusResourceTagName to 51,
            TagsConfig.DatabaseResourceTagName to 99,
            TagsConfig.AdminResourceTagName to 100
        )
    }


    override fun filterOpenAPI(openAPI: OpenAPI) {
        openAPI.tags = openAPI.tags.orEmpty().sortedWith { tagA, tagB ->
            (order[tagA.name] ?: 0).compareTo(order[tagB.name] ?: 0)
        }
    }

}