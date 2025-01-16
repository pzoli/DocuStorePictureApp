package hu.infokristaly.docustorepictureapp.network;

import hu.infokristaly.docustorepictureapp.model.DocInfo;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadAPIs
{
    @Multipart
    @POST("/api/file/upload")
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part file, @Part("doc") RequestBody doc);

    @POST("/api/docinfo")
    Call<DocInfo> sendDocInfo(@Body DocInfo docInfo);

}
