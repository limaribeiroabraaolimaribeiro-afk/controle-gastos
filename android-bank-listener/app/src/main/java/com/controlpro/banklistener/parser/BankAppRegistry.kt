package com.controlpro.banklistener.parser

import com.controlpro.banklistener.data.model.AllowedApp
import com.controlpro.banklistener.data.model.DefaultBankApps

object BankAppRegistry {

    private val bankNameByPackage: Map<String, String> = DefaultBankApps.list.associate {
        it.packageName to it.displayName
    }

    fun getBankName(packageName: String): String {
        return bankNameByPackage[packageName] ?: packageName.split(".").lastOrNull()
            ?.replaceFirstChar { it.uppercase() } ?: "Banco"
    }

    fun isKnownBank(packageName: String): Boolean {
        return bankNameByPackage.containsKey(packageName)
    }

    fun getAllApps(): List<AllowedApp> = DefaultBankApps.list

    fun isBankPackage(packageName: String, enabledPackages: Set<String>): Boolean {
        return packageName in enabledPackages
    }
}
