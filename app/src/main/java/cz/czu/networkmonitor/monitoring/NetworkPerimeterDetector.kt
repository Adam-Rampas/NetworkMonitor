package cz.czu.networkmonitor.monitoring

import android.content.Context
import android.net.ConnectivityManager
import java.net.InetAddress

class NetworkPerimeterDetector(context: Context) {
    private val connectivityManager = context.applicationContext
        .getSystemService((Context.CONNECTIVITY_SERVICE)) as ConnectivityManager

    fun detect() : NetworkPerimeter{
        val activeNetwork = connectivityManager.activeNetwork
            ?: return NetworkPerimeter.UNKNOWN

        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            ?: return NetworkPerimeter.UNKNOWN

        val addresses = linkProperties.linkAddresses.map { it.address }
            .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }

        if(addresses.isEmpty()){
            return NetworkPerimeter.UNKNOWN
        }

        if(addresses.any { isInZCUPerimeter(it) }) {
            return NetworkPerimeter.INTERNAL
        }

        return NetworkPerimeter.EXTERNAL
    }

    private fun isInZCUPerimeter(address: InetAddress): Boolean {
        val ipAddress = address.hostAddress?.lowercase() ?: return false

        if(ipAddress.startsWith("147.228.")) {
            return true
        }
        if(ipAddress.startsWith("2001:718:1801:")){
            return true
        }
        return false
    }
}