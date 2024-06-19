package hu.infokristaly.forrasimageserver.entity

class Subject (
    var id: Int?,
    var value: String
) {
    override fun toString(): String {
        return value
    }
}