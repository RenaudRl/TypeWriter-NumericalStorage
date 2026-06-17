package btcrenaud.numericalstorage

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider

/**
 * Bridge to Vault Economy API.
 * Isolated in its own file so that [NumericalStorageTransactionHandler]
 * never references Vault classes directly — avoiding NoClassDefFoundError
 * when Vault is not installed.
 */
@Suppress("DEPRECATION")
object VaultEconomyProvider {

    private val economy: Economy? by lazy {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return@lazy null
        val rsp: RegisteredServiceProvider<Economy>? =
            Bukkit.getServicesManager().getRegistration(Economy::class.java)
        rsp?.provider
    }

    val isAvailable: Boolean get() = economy != null

    fun getBalance(player: Player): Double = economy?.getBalance(player) ?: 0.0

    fun withdrawPlayer(player: Player, amount: Double): Boolean {
        val econ = economy ?: return false
        val response: EconomyResponse = econ.withdrawPlayer(player, amount)
        return response.transactionSuccess()
    }

    fun depositPlayer(player: Player, amount: Double): Boolean {
        val econ = economy ?: return false
        val response: EconomyResponse = econ.depositPlayer(player, amount)
        return response.transactionSuccess()
    }
}
