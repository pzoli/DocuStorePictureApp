package hu.infokristaly.docustorepictureapp.model

import hu.infokristaly.forrasimageserver.entity.DocumentSubject
import java.util.Date

enum class DocumentDirection {
    IN, OUT
}

data class DocInfo (
    var id: Long?,
    var subject: DocumentSubject,
    var direction: DocumentDirection,
    var organization: Organization,
    var clerk: Clerk?,
    var createdAt: Date?,
) {
}