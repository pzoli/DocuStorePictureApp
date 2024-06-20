package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hu.infokristaly.docustorepictureapp.model.Organization
import hu.infokristaly.forrasimageserver.entity.Subject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class ApiRoutins {

    companion object {

        fun getServerAddress(context: Context, packageName: String): String {
            val prefFile = "${packageName}_preferences"
            val sharedPreferences = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
            val result = sharedPreferences.getString("serveraddress", "") ?: ""
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
        fun getApiRequest(url: URL): String {
            val sslContext = getSSLContext()
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
            var result = ""
            try {
                val conn = url.openConnection() as HttpURLConnection
                with(conn) {
                    requestMethod = "GET"
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

        private fun deleteApiRequest(url: URL): String {
            var result = ""
            try {
                val sslContext = getSSLContext()
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
                val conn = url.openConnection() as HttpURLConnection
                with(conn) {
                    requestMethod = "DELETE"
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

        fun postPutApiRequest(url: URL, method: String, jsonInputString: String): String {
            var result = ""
            try {
                val sslContext = getSSLContext()
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(getHostnameVerifier())
                val conn = url.openConnection() as HttpURLConnection
                with(conn) {
                    requestMethod = method
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

        fun getSubjects(serverAddress: String): List<Subject> {
            var subjectList: List<Subject>
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = getApiRequest(URL("https://$serverAddress/api/subject"))
                        return@withContext result
                    }
                }
                val subjectsResult = result.await()
                try {
                    val gson = Gson()
                    val itemType = object : TypeToken<List<Subject>>() {}.type
                    subjectList = gson.fromJson<List<Subject>>(subjectsResult, itemType)
                } catch (e: Exception) {
                    subjectList = listOf()
                }
            }
            return subjectList
        }

        fun deleteSubject(serverAddress: String, id: Int) {
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = deleteApiRequest(URL("https://$serverAddress/api/subject/$id"))
                        return@withContext result
                    }
                }
                val subjectsResult = result.await()
            }
        }

        fun postPutSubject(url: String, method: String, jsonString: String): Subject? {
            var subject: Subject?
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = postPutApiRequest(URL(url), method, jsonString)
                        return@withContext result
                    }
                }
                val subjectResult = result.await()
                try {
                    val gson = Gson()
                    subject = gson.fromJson(subjectResult, Subject::class.java)
                } catch (e: Exception) {
                    Log.e("ApiRoutins", e.message.toString())
                    subject = null
                }
            }
            return subject
        }

        fun getOrganizations(serverAddress: String): List<Organization> {
            var organizationList: List<Organization>
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            ApiRoutins.getApiRequest(URL("https://$serverAddress/api/organization"))
                        return@withContext result
                    }
                }
                val organizationsResult = result.await()
                try {
                    val gson = Gson()
                    val itemType = object : TypeToken<List<Organization>>() {}.type
                    organizationList =
                        gson.fromJson<List<Organization>>(organizationsResult, itemType)
                } catch (e: Exception) {
                    organizationList = listOf()
                }
            }
            return organizationList
        }

        fun deleteOrganization(serverAddress: String, id: Int) {
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result =
                            deleteApiRequest(URL("https://$serverAddress/api/organization/$id"))
                        return@withContext result
                    }
                }
                result.await()
            }
        }

        fun postPutOrganization(url: String, method: String, jsonString: String): Organization? {
            var organization: Organization?
            runBlocking {
                var result: Deferred<String> = async() {
                    withContext(Dispatchers.IO) {
                        val result = postPutApiRequest(URL(url), method, jsonString)
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