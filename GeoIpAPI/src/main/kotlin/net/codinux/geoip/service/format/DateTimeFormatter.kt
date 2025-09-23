package net.codinux.geoip.service.format

import jakarta.inject.Singleton
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Singleton
class DateTimeFormatter {

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
    }


    fun formatDateTime(time: Instant?): String =
        if (time == null) {
            "-"
        } else {
            dateTimeFormatter.format(time.atZone(ZoneId.systemDefault()).toLocalDateTime())
        }

}