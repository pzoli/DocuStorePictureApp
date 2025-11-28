package hu.infokristaly.docustorepictureapp.model

import java.io.Serializable

data class DocLocation(
    var id: Long?,
    var name: String?,
    var parent: DocLocation?,
    ) : Serializable {
    override fun toString(): String {
        return name!!
    }
}