package hu.infokristaly.docustorepictureapp.model

import java.io.Serializable

data class Organization(
    var id: Long?,
    var name: String,
    var hqAddress: String,
    var hqPhone: String,

) : Serializable {
    override fun toString(): String {
        return name
    }
}
