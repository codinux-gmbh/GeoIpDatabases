package net.codinux.geoip.service

import jakarta.inject.Singleton
import net.codinux.geoip.service.format.DateTimeFormatter
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpDatabaseFileState
import net.codinux.geoip.service.model.GeoIpProviderDatabaseFileStates
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import net.codinux.geoip.service.model.status.AggregatedFileDownloadStatus
import net.codinux.geoip.service.model.status.FileDownloadStatus
import net.codinux.geoip.service.model.status.ProviderFileDownloadStatus
import net.codinux.geoip.service.model.status.ProvidersFileDownloadStatus
import java.time.Instant

@Singleton
class FileDownloadStatusService(
    private val state: GeoIpProvidersDatabaseFileState,
    private val dateTimeFormatter: DateTimeFormatter,
) {

    fun determineFileDownloadStatus(): ProvidersFileDownloadStatus {
        val geoLite2 = mapProviderFileDownloadStatus(state.geoLite2)
        val ipLocate = mapProviderFileDownloadStatus(state.ipLocate)

        return ProvidersFileDownloadStatus(getAggregatedStatus(geoLite2, ipLocate), geoLite2, ipLocate)
    }


    private fun mapProviderFileDownloadStatus(states: GeoIpProviderDatabaseFileStates): ProviderFileDownloadStatus {
        val asn = mapFileDownloadStatus(states.asn)
        val country = mapFileDownloadStatus(states.country)
        val city = mapFileDownloadStatus(states.city)

        return ProviderFileDownloadStatus(getAggregatedStatus(asn, country, city),
            asn, country, city)
    }

    private fun mapFileDownloadStatus(state: GeoIpDatabaseFileState) = FileDownloadStatus(
        state.downloadState,
        getDisplayMessage(state),
        state.downloadPath,
        state.lastDownloaded,
        state.lastUpdateFailedTime,
        state.lastUpdateFailedErrorMessage
    )

    private fun getDisplayMessage(state: GeoIpDatabaseFileState): String = when (state.downloadState) {
        DownloadFileState.UpToDate -> "Successfully downloaded at ${formatTime(state.lastDownloaded)}"
        DownloadFileState.DownloadedButUpdateFailed ->
            "Older file downloaded, but Update failed at ${formatTime(state.lastUpdateFailedTime)} with error: ${state.lastUpdateFailedErrorMessage}"
        DownloadFileState.NotAvailableForProvider -> "Not available for provider"
        DownloadFileState.DownloadDisabled -> "Download disabled"
        DownloadFileState.NotDownloadedYet -> {
            if (state.lastUpdateFailedErrorMessage != null) {
                "Not downloaded yet, last Update failed at ${formatTime(state.lastUpdateFailedTime)} with error: ${state.lastUpdateFailedErrorMessage}"
            } else {
                "Not downloaded"
            }
        }
    }

    private fun formatTime(time: Instant?): String =
        dateTimeFormatter.formatDateTime(time)

    private fun getAggregatedStatus(vararg status: FileDownloadStatus): AggregatedFileDownloadStatus =
        if (status.all { it.state in listOf(DownloadFileState.UpToDate, DownloadFileState.DownloadDisabled, DownloadFileState.NotAvailableForProvider) }) {
            AggregatedFileDownloadStatus.AllFilesUpToDate
        } else if (status.any { it.state == DownloadFileState.NotDownloadedYet }) {
            AggregatedFileDownloadStatus.AtLeastOneFileNotDownloadedYet
        } else {
            AggregatedFileDownloadStatus.AllFilesAtLeastDownloadedOnce
        }

    private fun getAggregatedStatus(geoLite2: ProviderFileDownloadStatus, ipLocate: ProviderFileDownloadStatus): AggregatedFileDownloadStatus =
        if (geoLite2.status == AggregatedFileDownloadStatus.AllFilesUpToDate && ipLocate.status == AggregatedFileDownloadStatus.AllFilesUpToDate) {
            AggregatedFileDownloadStatus.AllFilesUpToDate
        } else if (geoLite2.status == AggregatedFileDownloadStatus.AtLeastOneFileNotDownloadedYet || ipLocate.status == AggregatedFileDownloadStatus.AtLeastOneFileNotDownloadedYet) {
            AggregatedFileDownloadStatus.AtLeastOneFileNotDownloadedYet
        } else {
            AggregatedFileDownloadStatus.AllFilesAtLeastDownloadedOnce
        }

}