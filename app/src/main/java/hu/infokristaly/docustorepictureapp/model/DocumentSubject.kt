package hu.infokristaly.docustorepictureapp.model

import java.io.Serializable

class DocumentSubject (
    var id: Long?,
    var value: String
) : Serializable {
    override fun toString(): String {
        return value
    }
}