package cz.czu.networkmonitor.monitoring

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

class HttpChecker : NetworkChecker {
    override suspend fun checkNetwork(target: MonitorTarget): MonitorResult = withContext(Dispatchers.IO) {

        val address = target.address.trim()

        fun result(status: MonitorStatus, responseTimeMs: Double? = null, error: String? = null) =
            MonitorResult(
                targetId = target.id,
                target = address,
                status = status,
                networkPerimeter = NetworkPerimeter.UNKNOWN,
                responseTimeMs = responseTimeMs,
                timeStamp = System.currentTimeMillis(),
                errorMessage = error
            )

        val uri = try {
            URI(address)
        } catch (e: Exception) {
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        if(uri.scheme?.lowercase() !in listOf("http", "https")){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Podporované protokoly jsou pouze HTTP a HTTPS."
            )
        }

        if(uri.host.isNullOrBlank()){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        val url = try{
            uri.toURL()
        } catch (e: Exception){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Adresu nelze převést na URL: "
            )
        }

        var connection: HttpURLConnection? = null

        try {
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.useCaches = false


            val startTime = SystemClock.elapsedRealtimeNanos()

            val responseCode = connection.responseCode

            val endTime = SystemClock.elapsedRealtimeNanos()

            val responseTimeMs = (endTime - startTime) / 1_000_000.0

            if(responseCode in 200..399){
                result(
                    status = MonitorStatus.AVAILABLE,
                    responseTimeMs = responseTimeMs
                )
            }else {
                result(
                    status = MonitorStatus.UNAVAILABLE,
                    responseTimeMs = responseTimeMs,
                    error = "HTTP odpověď: $responseCode"
                )
            }
        }
        catch (e: SocketTimeoutException) {
            result(
                status = MonitorStatus.UNAVAILABLE,
                error = "Časový limit vypršel"
            )
        }
        catch (e: IOException) {
            result(
                status = MonitorStatus.ERROR,
                error = "Chyba při provádění HTTP požadavku "
            )
        } finally {
            connection?.disconnect()
        }
    }
}