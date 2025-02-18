package hu.infokristaly.docustorepictureapp.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import hu.infokristaly.docustorepictureapp.DocInfoActivity;
import hu.infokristaly.docustorepictureapp.MainActivity;
import hu.infokristaly.docustorepictureapp.model.DocInfo;
import hu.infokristaly.docustorepictureapp.model.FileInfo;
import hu.infokristaly.docustorepictureapp.utils.ApiRoutins;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import hu.infokristaly.docustorepictureapp.R;

public class NetworkClient {
    private Retrofit retrofit;

    public static OkHttpClient getOkHttpClient(Context context) {
        try {
            // Create a trust manager that does not validate the certificate
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            // Create an SSL context that uses the trust manager
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());

            String username = ApiRoutins.Companion.getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME));
            String password = ApiRoutins.Companion.getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD));
            // Create the OkHttpClient builder and set the SSL socket factory
            OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
            okHttpBuilder.addInterceptor(new BasicAuthInterceptor(username, password));
            okHttpBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            okHttpBuilder.hostnameVerifier((hostname, session) -> true);

            return okHttpBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Retrofit getRetrofitClient(Context context, String BASE_URL) {

        if (retrofit == null) {
            //OkHttpClient okHttpClient = new OkHttpClient.Builder()
            //        .build();
            OkHttpClient okHttpClient = getOkHttpClient(context);
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public void sendDocInfo(Context context, DocInfo docInfo) {
        try {
            String serverAddress = ApiRoutins.Companion.getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS));
            Retrofit retrofit = getRetrofitClient(context, "https://" + serverAddress);

            UploadAPIs uploadAPIs = retrofit.create(UploadAPIs.class);
            Call call = uploadAPIs.sendDocInfo(docInfo);

            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    Log.e("NetworkClient", response.message());
                    if (response.code() == 201) {
                        Toast.makeText(context, "DocInfo successfully uploaded", Toast.LENGTH_LONG).show();
                        ((DocInfoActivity)context).stored.docInfo = ((DocInfo)response.body());
                        Intent intent = new Intent(context, MainActivity.class);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "DocInfo upload with response code " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    Log.e("NetworkClient", t.getLocalizedMessage());
                    Toast.makeText(context, "DocInfo uploaded failed: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "File uploaded failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

   public void uploadToServer(Context context, DocInfo docInfo, String filePath) {
        try {
            String serverAddress = ApiRoutins.Companion.getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS));
            Retrofit retrofit = getRetrofitClient(context, "https://" + serverAddress);

            UploadAPIs uploadAPIs = retrofit.create(UploadAPIs.class);

            File file = new File(filePath);

            RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileReqBody);

            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").create();
            String jsonObject = gson.toJson(docInfo);

            RequestBody docInfoPart = RequestBody.create(MediaType.parse("application/json"), jsonObject);

            Call<ResponseBody> call = uploadAPIs.uploadImage(filePart, docInfoPart);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call call,@NonNull Response response) {
                    Log.e("NetworkClient", response.message());
                    if (response.code() == 200) {
                        if (response.body() != null) {
                            try {
                                String body = ((ResponseBody) response.body()).string();
                                FileInfo fileInfo = gson.fromJson(body, FileInfo.class);
                                String newFileName = ((MainActivity) context).getIMAGENAME_FROM_SERVER() + "." + file.getName().substring(file.getName().indexOf(".") + 1);
                                File target = new File(file.getParentFile().getAbsolutePath(), newFileName);
                                Files.move(file.toPath(), target.toPath());
                                Toast.makeText(context, "File successfully uploaded", Toast.LENGTH_LONG).show();
                                ((MainActivity) context).setFileInfo(fileInfo);
                                ((MainActivity) context).setFileName(target.getAbsolutePath());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        Toast.makeText(context, "File uploaded failed with code("+response.code()+")", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                    Log.e("NetworkClient", t.getLocalizedMessage());
                    Toast.makeText(context, "File uploaded failed: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "File uploaded failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void updateOnServer(Context context, long fileInfoId, String filePath) {
        try {
            String serverAddress = ApiRoutins.Companion.getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS));
            Retrofit retrofit = getRetrofitClient(context, "https://" + serverAddress);
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").create();

            UploadAPIs uploadAPIs = retrofit.create(UploadAPIs.class);

            File file = new File(filePath);
            RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileReqBody);

            Call<ResponseBody> call = uploadAPIs.updateImage(filePart, fileInfoId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    Log.e("NetworkClient", response.message());
                    if (response.code() == 200) {
                        if (response.body() != null) {
                            FileInfo fileInfo = gson.fromJson(response.body().toString(), FileInfo.class);
                            Toast.makeText(context, "File ("+fileInfo.getId()+") successfully updated", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, "File uploaded failed with code("+response.code()+")", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e("NetworkClient", t.getLocalizedMessage());
                    Toast.makeText(context, "File uploaded failed: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "File uploaded failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
