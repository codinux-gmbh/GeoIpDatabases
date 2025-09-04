package net.codinux.geoip.config

import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.Path


fun <T, R> Optional<T>.mapOrNull(mapper: (T) -> R?): R? =
    this.map { mapper(it) }.orElse(null)

fun Optional<String>.toPathOrNull(): Path? =
    this.mapOrNull { Path(it) }