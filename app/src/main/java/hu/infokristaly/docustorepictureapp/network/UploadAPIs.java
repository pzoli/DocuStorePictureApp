package hu.infokristaly.docustorepictureapp.network;

import hu.infokristaly.docustorepictureapp.model.DocInfo;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface UploadAPIs
{
    @Multipart
    @POST("/api/file/upload")
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part file, @Part("doc") RequestBody doc);

    @Multipart
    @PUT("/api/file/update/{id}")
    Call<ResponseBody> updateImage(@Part MultipartBody.Part file, @Path("id") long id);

    @POST("/api/docinfo")
    Call<DocInfo> sendDocInfo(@Body DocInfo docInfo);

    @PUT("/api/docinfo/{id}")
    Call<DocInfo> updateDocInfo(@Body DocInfo docInfo, @Path("id") long id);

}
