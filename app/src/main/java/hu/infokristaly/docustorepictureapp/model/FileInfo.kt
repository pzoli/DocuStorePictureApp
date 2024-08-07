package hu.infokristaly.docustorepictureapp.model

data class FileInfo(
     var id:Long?,
     var uniqueFileName:String,
     var lenght:Long,
     var docInfo:DocInfo,
) {
}