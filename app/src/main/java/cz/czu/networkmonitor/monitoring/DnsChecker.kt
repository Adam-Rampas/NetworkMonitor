package cz.czu.networkmonitor.monitoring

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException


class DnsChecker : NetworkChecker {
    override suspend fun checkNetwork(target: MonitorTarget): MonitorResult = withContext(Dispatchers.IO){

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

        if(address.isBlank() || address.startsWith("-") || address.any{it.isWhitespace() || it.isISOControl()}){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }
        try {
            val startTime = SystemClock.elapsedRealtimeNanos()
            val intAdresses = InetAddress.getAllByName(address)
            val endTime = SystemClock.elapsedRealtimeNanos()
            val responseTimeMs = (endTime - startTime) / 1_000_000.0 // převod na milisekundy

            if(intAdresses.isNotEmpty()){
                return@withContext result(
                    status =  MonitorStatus.AVAILABLE,
                    responseTimeMs = responseTimeMs
                )
            }
            else {
                return@withContext result(
                    status = MonitorStatus.UNAVAILABLE,
                    responseTimeMs = responseTimeMs,
                    error = "DNS záznam nenalezen"
                )
            }
        }
        catch (e: UnknownHostException){
            result(
                status = MonitorStatus.UNAVAILABLE,
                error = "DNS záznam nenalezen: ${e.message}"
            )
        }
        catch (e: Exception){
            result(
                status = MonitorStatus.ERROR,
                error = "Chyba při DNS dotazu: ${e.message}"
            )
        }
    }
}