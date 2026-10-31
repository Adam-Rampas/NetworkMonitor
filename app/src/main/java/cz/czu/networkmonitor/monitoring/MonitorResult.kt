package cz.czu.networkmonitor.monitoring

enum class MonitorStatus {
    AVAILABLE,
    UNAVAILABLE,
    ERROR
}

enum class NetworkPerimeter{
    INTERNAL,
    EXTERNAL,
    UNKNOWN
}

data class MonitorResult(
    val targetId: Long?,
    val target: String,
    val status: MonitorStatus,
    val networkPerimeter: NetworkPerimeter,
    val responseTimeMs: Double?,
    val timeStamp: Long,
    val errorMessage: String?
)
