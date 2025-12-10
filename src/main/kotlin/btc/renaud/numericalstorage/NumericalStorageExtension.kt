package btc.renaud.numericalstorage

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.plugin
import org.bukkit.Bukkit

@Singleton
class NumericalStorageExtension : Initializable {

    private val interestService = NumericalStorageInterestService(plugin)
    private val menuListener = NumericalStorageMenuListener(plugin)

    override suspend fun initialize() {
        interestService.register()
        menuListener.register()
    }

    override suspend fun shutdown() {
        // No specific shutdown logic needed yet
    }
}
