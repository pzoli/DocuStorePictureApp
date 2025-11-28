package hu.infokristaly.docustorepictureapp.model

import java.io.Serializable
import java.util.Date

enum class DocumentDirection {
    IN, OUT
}

data class DocInfo (
    var id: Long?,
    var subject: DocumentSubject?,
    var direction: DocumentDirection?,
    var organization: Organization?,
    var clerk: Clerk?,
    var createdAt: Date?,
    var comment: String?,
    var docLocation: DocLocation?,
) : Serializable {
    override fun toString(): String {
        return subject.toString() + " - " + organization.toString()
    }
}
