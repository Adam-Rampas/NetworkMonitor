package cz.czu.networkmonitor

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cz.czu.networkmonitor.monitoring.MonitorTarget
import cz.czu.networkmonitor.monitoring.PingChecker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            val checker = PingChecker()

            val target = MonitorTarget(address = "127.0.0.1", id = 1)
            val result = checker.checkNetwork(target)

            Log.d("PING_TEST", "Cíl:  ${result.target}")
            Log.d("PING_TEST", "Stav:  ${result.status}")
            Log.d("PING_TEST", "Odezva:  ${result.responseTimeMs} ms")
            Log.d("PING_TEST", "Chyba:  ${result.errorMessage}")

        }
    }
}