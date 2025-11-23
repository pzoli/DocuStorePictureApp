package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.DocInfo
import hu.infokristaly.docustorepictureapp.model.DocumentSubject
import hu.infokristaly.docustorepictureapp.model.FileInfo
import hu.infokristaly.docustorepictureapp.model.Organization
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Optional
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


class ApiRoutins {

    companion object {
        fun getSharedPrefProp(context: Context, key: String): String {
            val prefFile = "${context.packageName}_preferences"
            val sharedPreferences = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
            val result = sharedPreferences.getString(key , "") ?: ""
            return result
        }

        fun getTrustManager(): X509TrustManager {
            return object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        }
        fun getSSLContext(): SSLContext {
            val trustManager = getTrustManager()
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustManager), SecureRandom())
            return sslContext
        }

        fun getHostnameVerifier(): HostnameVerifier {
            return HostnameVerifier { _, _ -> true }
        }
        fun getApiRequest(url: URL, userName: String, password:String): String {
            val sslContext = getSSLContext()
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
            var result = ""
            val userCredentials = "$userName:$password"
            val basicAuth = "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))
            val conn = url.openConnection() as HttpURLConnection
            with(conn) {
                requestMethod = "GET"
                connectTimeout = 1000
                setRequestProperty("Authorization", basicAuth)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.bufferedReader().use {
                        it.lines().forEach { line ->
                            result += line
                        }
                    }
                }
            }

            return result;
        }

        fun getApiRequestAsByteArrayResult(url: URL, userName: String, password:String): ByteArray {
            val sslContext = getSSLContext()
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
            var result = ByteArray(0)
            val userCredentials = "$userName:$password"
            val basicAuth = "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))
            val conn = url.openConnection() as HttpURLConnection
            with(conn) {
                requestMethod = "GET"
                connectTimeout = 1000
                setRequestProperty("Authorization", basicAuth)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    result = IOUtils.toByteArray(inputStream)
                }
            }
            return result;
        }


        private fun deleteApiRequest(url: URL, userName: String, password:String): String {
            var result = ""
            val userCredentials = "$userName:$password"
            val basicAuth = "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))
            val sslContext = getSSLContext()
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
            val conn = url.openConnection() as HttpURLConnection
            with(conn) {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", basicAuth)
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    inputStream.bufferedReader().use {
                        it.lines().forEach { line ->
                            result += line
                        }
                    }
                }
            }
            return result;
        }

        fun postPutApiRequest(url: URL, method: String, jsonInputString: String, userName: String, password:String): String {
            var result = ""
            val userCredentials = "$userName:$password"
            val basicAuth = "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))
            val sslContext = getSSLContext()
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
            val conn = url.openConnection() as HttpURLConnection
            with(conn) {
                requestMethod = method
                setRequestProperty("Authorization", basicAuth)
                setRequestProperty("Content-Type", "application/json");
                setRequestProperty("Accept", "application/json");
                outputStream.use { os ->
                    val input: ByteArray = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    inputStream.bufferedReader().use {
                        it.lines().forEach { line ->
                            result += line
                        }
                    }
                } else {
                    Log.e("ApiRoutins", "Response code: ${responseCode}")
                }
            }
            return result;
        }

        fun getSubjects(context: Context): Optional<List<DocumentSubject>> {
            var subjectList: Optional<List<DocumentSubject>> = Optional.empty()
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result = getApiRequest(URL("https://$serverAddress/api/subject"),userName,password)
                        return@withContext result
                    }
                }
                val subjectsResult = result.await()
                val gson = Gson()
                val itemType = object : TypeToken<List<DocumentSubject>>() {}.type
                subjectList = Optional.of(gson.fromJson<List<DocumentSubject>>(subjectsResult, itemType))
            }
            return subjectList
        }

        fun deleteSubject(context: Context, id: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result = deleteApiRequest(
                            URL("https://$serverAddress/api/subject/$id"),
                            userName,
                            password)
                        return@withContext result
                    }
                }
                result.await()
            }
        }

        fun postPutSubject(context: Context, url: String, method: String, jsonString: String): Optional<DocumentSubject> {
            var subject: Optional<DocumentSubject> = Optional.empty()
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                            val result = postPutApiRequest(
                                URL(url),
                                method,
                                jsonString,
                                userName,
                                password
                            )
                            return@withContext result
                    }
                }
                val subjectResult = result.await()
                val gson = Gson()
                subject = Optional.of(gson.fromJson(subjectResult, DocumentSubject::class.java))
            }
            return subject
        }

        fun getOrganizations(context: Context): Optional<List<Organization>> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            var organizationList: Optional<List<Organization>> = Optional.empty()
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            getApiRequest(
                                URL("https://$serverAddress/api/organization"),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }
                val organizationsResult = result.await()
                val gson = Gson()
                val itemType = object : TypeToken<List<Organization>>() {}.type
                organizationList =
                    Optional.of(gson.fromJson(organizationsResult, itemType))
            }
            return organizationList
        }

        fun deleteOrganization(context: Context, id: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            deleteApiRequest(
                                URL("https://$serverAddress/api/organization/$id"),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }
                result.await()
            }
        }

        fun postPutOrganization(context: Context,url: String, method: String, jsonString: String): Optional<Organization> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))

            var organization: Optional<Organization> = Optional.empty()
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result = postPutApiRequest(
                            URL(url),
                            method,
                            jsonString,
                            userName,
                            password
                        )
                        return@withContext result
                    }
                }
                val organizationResult = result.await()
                val gson = Gson()
                organization = Optional.of(gson.fromJson(organizationResult, Organization::class.java))
            }
            return organization
        }

        fun deleteDocInfo(context: Context, id: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                            val result =
                                deleteApiRequest(
                                    URL("https://$serverAddress/api/docinfo/$id"),
                                    userName,
                                    password
                                )
                            return@withContext result
                    }
                }
                result.await()
            }
        }

        @Throws(Exception::class)
        fun getDocInfos(context: Context): Optional<List<DocInfo>> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            var docInfoList: Optional<List<DocInfo>> = Optional.empty()
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            getApiRequest(
                                URL("https://$serverAddress/api/docinfo"),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }

                val docInfosResult = result.await()
                val gson = Gson()
                val itemType = object : TypeToken<List<DocInfo>>() {}.type
                docInfoList = Optional.of(gson.fromJson(docInfosResult, itemType))
            }
            return docInfoList
        }

        fun getImage(context: Context, id: Long): Optional<ByteArray> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            var imageResult: ByteArray
            runBlocking {
                var result: Deferred<ByteArray> = async() {
                withContext(Dispatchers.IO) {
                        val result =
                            getApiRequestAsByteArrayResult(
                                URL("https://$serverAddress/api/file/" + id.toString()),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }
                imageResult = result.await()
            }
            return Optional.of(imageResult)
        }

        fun getFileInfosForDocInfo(context: Context, id: Long?): Optional<List<FileInfo>> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            var fileInfoList: Optional<List<FileInfo>> = Optional.empty()
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            getApiRequest(
                                URL("https://$serverAddress/api/fileinfo/bydocinfoid/" + id),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }
                val fileInfosResult = result.await()
                val gson = Gson()
                val itemType = object : TypeToken<List<FileInfo>>() {}.type
                fileInfoList =
                    Optional.of(gson.fromJson(fileInfosResult, itemType))
            }
            return fileInfoList
        }

        fun deleteFileInfo(context: Context, fileInfoId: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String?> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            deleteApiRequest(
                                URL("https://$serverAddress/api/fileinfo/${fileInfoId}"),
                                userName,
                                password
                            )
                        return@withContext result
                    }
                }
                result.await()
            }
        }

    }

}