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

    fun getLocatoinPath(): String {
        val result = StringBuilder(name)
        var parentLocation  = parent
        while(parentLocation != null) {
            result.insert(0," - ")
            result.insert(0, parentLocation!!.name)
            parentLocation = parentLocation!!.parent
        }
        return result.toString()
    }

}