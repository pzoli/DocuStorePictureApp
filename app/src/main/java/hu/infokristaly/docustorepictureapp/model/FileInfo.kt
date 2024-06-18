package hu.infokristaly.docustorepictureapp.model

data class FileInfo(
     var id:Int?,
     var uniqueFileName:String,
     var lenght:Long,
     var docInfo:DocInfo,
) {
}