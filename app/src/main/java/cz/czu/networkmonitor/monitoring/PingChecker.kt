package cz.czu.networkmonitor.monitoring

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class PingChecker : NetworkChecker {
    override  suspend fun checkNetwork(target: MonitorTarget): MonitorResult = withContext(Dispatchers.IO){
        val address = target.address.trim()

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

        //kontrola platnosti adresy, pokud je adresa prazdna nebo null, vratime chybu
        if(address.isBlank() || address.startsWith("-") || address.any{ it.isWhitespace() || it.isISOControl()}){
            return@withContext result(
                status = MonitorStatus.ERROR,
                error = "Neplatná adresa: '$address'"
            )
        }

        //vytvorime si process pro ping, null dokud neni spusten
        var process : Process? = null
        try {
            //spustime prikaz ping (-n neprekladej ip, -c 1 = 1 paket, -W 2 = timeout 2s)
            process = ProcessBuilder("/system/bin/ping", "-n", "-c", "1", "-W", "2", address)
                .redirectErrorStream(true)
                .start()

            //zmerime cas spusteni prikazu
            val completed = runInterruptible {
                process.waitFor(5, TimeUnit.SECONDS)
            }

            //pokud se proces neukoncil do limitu, vratime chybu
            if(!completed){
                return@withContext result(
                    status = MonitorStatus.ERROR,
                    error = "Měření překročilo časový limit"
                )
            }

            //precteme si text, ktery ping vrati
            val output = process.inputStream.bufferedReader().use { it.readText() }
            when(val exitCode = process.exitValue()){
                0 -> {
                    //regex pro ziskani doby odezvy v ms z vystupu prikazu ping
                    val regex = Regex("""time[=<]([\d.,]+)\s*ms""")

                    //ziskame hodnotu odezvy a carku nahradime teckou, kvuli Double
                    val responseTime = regex.find(output)
                        ?.groupValues?.get(1)
                        ?.replace(",", ".")
                        ?.toDoubleOrNull() //prevedeme na Double

                    //pokud ziskame hodnotu odezvy, vratime vysledek s dostupnosti
                    result(
                        status = MonitorStatus.AVAILABLE,
                        responseTimeMs = responseTime
                    )
                }
                1 -> {
                    //pokud se nepodarilo ziskat, vratime vysledek s nedostupnosti
                    result(
                        status = MonitorStatus.UNAVAILABLE,
                        error = "Cílový hostitel je nedostupný"
                    )
                }
                else -> {
                    //pokud nastane jina chyba, vratime vysledek s chybou
                    result(
                        status = MonitorStatus.ERROR,
                        error = "Chyba při provádění příkazu ping (exit code: $exitCode) $output"
                    )
                }
            }

        } catch (e: IOException) {
            //pokud nastane chyba, vratime chybu
            result(
                status = MonitorStatus.ERROR,
                error = "Chyba při provádění příkazu ping: ${e.message}"
            )
        } finally {
            process?.destroyForcibly() //ukoncime proces
        }
    }
}