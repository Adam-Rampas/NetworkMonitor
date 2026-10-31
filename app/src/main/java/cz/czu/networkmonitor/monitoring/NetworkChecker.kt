package cz.czu.networkmonitor.monitoring

/**
 *  Společný interface pro všechny třídy, které kontrolují stav sítě.
 *
 */
interface NetworkChecker {
    suspend fun checkNetwork(target: MonitorTarget): MonitorResult
}