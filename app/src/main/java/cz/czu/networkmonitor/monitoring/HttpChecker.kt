package cz.czu.networkmonitor.monitoring

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

/**
 * POZNÁMKA: URI jsme použili, protože je jednodušší pro získání hostitele a portu z URL
 * než ručně parsovat řetězec. URI také poskytuje metody pro získání jednotlivých částí URL, jako je hostitel, port, cesta atd.
 */


class HttpChecker : NetworkChecker {
    override suspend fun checkNetwork(target: MonitorTarget): MonitorResult = withContext(Dispatchers.IO) {

        val address = target.address.trim() //odstranime mezery

        //funkce pro zjednoduseni a vytvoreni vysledku
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

        //prevedeme si adresu na URI, pokud je neplatna, vratime chybu
        val uri = try {
            URI(address)
        } catch (e: Exception) {
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        //zkontrolujeme, zda je adresa platna pro HTTP/HTTPS, pokud ne, vratime chybu
        if(uri.scheme?.lowercase() !in listOf("http", "https")){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Podporované protokoly jsou pouze HTTP a HTTPS."
            )
        }

        //zametime, aby se nezadala prazdna adresa, napriklad "http://"
        if(uri.host.isNullOrBlank()){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        //z uri si vytvorime URL a otevreme spojeni
        val url = try{
            uri.toURL()
        } catch (e: Exception){ //pokud se nepovede, vratime chybu
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Adresu nelze převést na URL: "
            )
        }

        //vytvorime si http spojeni
        var connection: HttpURLConnection? = null

        try {
            connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 //5s na navazani spojeni
            connection.readTimeout = 5000 //5s na cteni odpovedi
            connection.instanceFollowRedirects = false //zakazeme redirect
            connection.useCaches = false //nechceme cache

            //zmerime cas odezvy
            val startTime = SystemClock.elapsedRealtimeNanos()

            val responseCode = connection.responseCode

            val endTime = SystemClock.elapsedRealtimeNanos()

            //prevedeme cas na milisekundy
            val responseTimeMs = (endTime - startTime) / 1_000_000.0

            //pokud je response code 200-399, vratime dostupnost
            if(responseCode in 200..399){
                result(
                    status = MonitorStatus.AVAILABLE,
                    responseTimeMs = responseTimeMs
                )
            }else {
                //pokud je response code mimo 200-399, vratime nedostupnost
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