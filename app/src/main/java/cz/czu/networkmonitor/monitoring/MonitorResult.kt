package cz.czu.networkmonitor.monitoring
/**
 * výsledek kontroly sítě pro daný cíl
 */
enum class MonitorStatus {
    AVAILABLE,
    UNAVAILABLE,
    ERROR
}

/**
 * Reprezentace perimetru sítě.
 */
enum class NetworkPerimeter{
    INTERNAL,
    EXTERNAL,
    UNKNOWN
}

/**
 * Datová třída, reprezentuje výsledky měření stavu sítě
 */
data class MonitorResult(
    val targetId: Long?,
    val target: String,
    val status: MonitorStatus,
    val networkPerimeter: NetworkPerimeter,
    val responseTimeMs: Double?,
    val timeStamp: Long,
    val errorMessage: String?
)
