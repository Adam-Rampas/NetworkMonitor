package cz.czu.networkmonitor.monitoring

data class MonitorTarget(
    val id: Long?,
    val address: String
)

enum class MonitorTargetType {
    DNS,
    HTTP,
    PING
}
