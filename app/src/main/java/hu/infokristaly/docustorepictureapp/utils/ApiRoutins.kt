package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.forrasimageserver.entity.DocumentSubject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
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
            try {
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
            } catch (e: Exception) {
                Log.e("Exception", e.toString())
            }

            return result;
        }

        private fun deleteApiRequest(url: URL, userName: String, password:String): String {
            var result = ""
            val userCredentials = "$userName:$password"
            val basicAuth =
                "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))
            try {
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
            } catch (e: Exception) {
                Log.e("Exception", e.toString())
            }

            return result;
        }

        fun postPutApiRequest(url: URL, method: String, jsonInputString: String, userName: String, password:String): String {
            var result = ""
            val userCredentials = "$userName:$password"
            val basicAuth = "Basic " + String(Base64.getEncoder().encode(userCredentials.toByteArray()))

            try {
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
            } catch (e: Exception) {
                Log.e("Exception", e.toString())
            }

            return result;
        }

        fun getSubjects(context: Context): List<DocumentSubject> {
            var subjectList: List<DocumentSubject>
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = getApiRequest(URL("https://$serverAddress/api/subject"),userName,password)
                        return@withContext result
                    }
                }
                val subjectsResult = result.await()
                try {
                    val gson = Gson()
                    val itemType = object : TypeToken<List<DocumentSubject>>() {}.type
                    subjectList = gson.fromJson<List<DocumentSubject>>(subjectsResult, itemType)
                } catch (e: Exception) {
                    subjectList = listOf()
                }
            }
            return subjectList
        }

        fun deleteSubject(context: Context, id: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = deleteApiRequest(URL("https://$serverAddress/api/subject/$id"), userName, password)
                        return@withContext result
                    }
                }
                val subjectsResult = result.await()
            }
        }

        fun postPutSubject(context: Context, url: String, method: String, jsonString: String): DocumentSubject? {
            var subject: DocumentSubject?
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = postPutApiRequest(URL(url), method, jsonString, userName, password)
                        return@withContext result
                    }
                }
                val subjectResult = result.await()
                try {
                    val gson = Gson()
                    subject = gson.fromJson(subjectResult, DocumentSubject::class.java)
                } catch (e: Exception) {
                    Log.e("ApiRoutins", e.message.toString())
                    subject = null
                }
            }
            return subject
        }

        fun getOrganizations(context: Context): List<Organization> {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            var organizationList: List<Organization>
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            getApiRequest(URL("https://$serverAddress/api/organization"), userName, password)
                        return@withContext result
                    }
                }
                val organizationsResult = result.await()
                try {
                    val gson = Gson()
                    val itemType = object : TypeToken<List<Organization>>() {}.type
                    organizationList =
                        gson.fromJson(organizationsResult, itemType)
                } catch (e: Exception) {
                    organizationList = listOf()
                }
            }
            return organizationList
        }

        fun deleteOrganization(context: Context, id: Long) {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))
            val serverAddress = getSharedPrefProp(context, context.getString(R.string.KEY_SERVERADDRESS))
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            deleteApiRequest(URL("https://$serverAddress/api/organization/$id"), userName, password)
                        return@withContext result
                    }
                }
                result.await()
            }
        }

        fun postPutOrganization(context: Context,url: String, method: String, jsonString: String): Organization? {
            val userName = getSharedPrefProp(context, context.getString(R.string.KEY_USERNAME))
            val password = getSharedPrefProp(context, context.getString(R.string.KEY_PASSWORD))

            var organization: Organization?
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = postPutApiRequest(URL(url), method, jsonString, userName, password)
                        return@withContext result
                    }
                }
                val organizationResult = result.await()
                try {
                    val gson = Gson()
                    organization = gson.fromJson(organizationResult, Organization::class.java)
                } catch (e: Exception) {
                    Log.e("ApiRoutins", e.message.toString())
                    organization = null
                }
            }
            return organization
        }

    }

}