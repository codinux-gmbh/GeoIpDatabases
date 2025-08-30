package net.codinux.geoip.database

enum class Continent(val code: String) {
    Africa("AF"),

    Antarctica("AN"),

    Asia("AS"),

    Europe("EU"),

    Oceania("OC"),

    NorthAmerica("NA"),

    SouthAmerica("SA")
    ;


    companion object {
        val byCode: Map<String, Continent> = entries.associateBy(Continent::code)

        fun byCode(code: String): Continent? = byCode[code]
    }

}