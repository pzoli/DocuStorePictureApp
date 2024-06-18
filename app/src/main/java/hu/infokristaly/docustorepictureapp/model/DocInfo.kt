package hu.infokristaly.docustorepictureapp.model

import hu.infokristaly.forrasimageserver.entity.Subject
import java.util.Date

enum class DocumentDirection {
    IN, OUT
}

data class DocInfo (
    var id: Int?,
    var subject: Subject,
    var direction: DocumentDirection,
    var organization: Organization,
    var clerk: Clerk?,
    var createdAt: Date?,
) {
}