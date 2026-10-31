package cz.czu.networkmonitor.monitoring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class PingChecker : NetworkChecker {
    override  suspend fun checkNetwork(target: MonitorTarget): MonitorResult = withContext(Dispatchers.IO){
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

        if(address.isBlank() || address.startsWith("-") || address.any{ it.isWhitespace() || it.isISOControl()}){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        var process : Process? = null
        try {
            process = ProcessBuilder("/system/bin/ping", "-n", "-c", "1", "-W", "2", address)
                .redirectErrorStream(true)
                .start()

            val completed = runInterruptible {
                process.waitFor(5, TimeUnit.SECONDS)
            }

            if(!completed){
                return@withContext result(
                    status = MonitorStatus.ERROR,
                    error = "Měření překročilo časový limit"
                )
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            when(val exitCode = process.exitValue()){
                0 -> {
                    val regex = Regex("""time[=<]([\d.,]+)\s*ms""")

                    val responseTime = regex.find(output)
                        ?.groupValues?.get(1)
                        ?.replace(",", ".")
                        ?.toDoubleOrNull()

                    result(
                        status = MonitorStatus.AVAILABLE,
                        responseTimeMs = responseTime
                    )
                }
                1 -> {
                    result(
                        status = MonitorStatus.UNAVAILABLE,
                        error = "Cílový hostitel je nedostupný"
                    )
                }
                else -> {
                    result(
                        status = MonitorStatus.ERROR,
                        error = "Chyba při provádění příkazu ping (exit code: $exitCode) $output"
                    )
                }
            }
        } catch (e: IOException) {
            result(
                status = MonitorStatus.ERROR,
                error = "Chyba při provádění příkazu ping: ${e.message}"
            )
        } finally {
            process?.destroyForcibly()
        }
    }
}