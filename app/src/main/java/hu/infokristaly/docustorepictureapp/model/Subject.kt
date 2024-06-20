package hu.infokristaly.forrasimageserver.entity

import java.io.Serializable

class Subject (
    var id: Int?,
    var value: String
) : Serializable {
    override fun toString(): String {
        return value
    }
}