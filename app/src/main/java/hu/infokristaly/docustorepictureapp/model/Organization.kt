package hu.infokristaly.docustorepictureapp.model

import java.io.Serializable

data class Organization(
    var id: Long?,
    var name: String,
    var hqAddress: String,
    var hqPhone: String,
    var version: Long?

) : Serializable {
    override fun toString(): String {
        return name
    }
}
