package com.controlpro.banklistener.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.controlpro.banklistener.data.local.AppPreferences
import com.controlpro.banklistener.data.local.SecurePreferences
import com.controlpro.banklistener.data.model.AllowedApp
import com.controlpro.banklistener.data.model.BankNotificationImport
import com.controlpro.banklistener.data.model.DefaultBankApps
import com.controlpro.banklistener.data.repository.AuthRepository
import com.controlpro.banklistener.data.repository.NotificationRepository
import com.controlpro.banklistener.domain.usecase.ConfirmImportUseCase
import com.controlpro.banklistener.parser.NotificationParser
import com.controlpro.banklistener.service.BankNotificationListenerService
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserInfo? = null,
    val userEmail: String? = null,
    val notificationPermission: Boolean = false,
    val serviceConnected: Boolean = false,
    val pendingImports: List<BankNotificationImport> = emptyList(),
    val recentImports: List<BankNotificationImport> = emptyList(),
    val allowedApps: List<AllowedApp> = DefaultBankApps.list,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val securePrefs = SecurePreferences(context)
    val authRepo = AuthRepository(securePrefs)
    private val notifRepo = NotificationRepository()
    private val confirmUseCase = ConfirmImportUseCase(notifRepo, authRepo)
    val appPrefs = AppPreferences(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val autoMode = appPrefs.autoModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val enabledPackages = appPrefs.enabledPackagesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val minConfidence = appPrefs.minConfidenceFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 85)

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val restored = authRepo.restoreSession()
            if (restored) {
                val email = authRepo.getUserEmail()
                _uiState.update { it.copy(isLoggedIn = true, userEmail = email, isLoading = false) }
                loadImports()
            } else {
                _uiState.update { it.copy(isLoggedIn = false, isLoading = false) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepo.signIn(email, password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true, userEmail = authRepo.getUserEmail())
                }
                loadImports()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Erro ao fazer login")
                }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepo.signUp(email, password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, successMessage = "Conta criada! Verifique seu e-mail e faça login.")
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "Erro ao criar conta")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.signOut()
            _uiState.update { UiState() }
        }
    }

    fun loadImports() {
        val userId = authRepo.getUserId() ?: return
        viewModelScope.launch {
            val pending = notifRepo.getPendingImports(userId)
            val recent = notifRepo.getRecentImports(userId)
            _uiState.update {
                it.copy(
                    pendingImports = pending.getOrDefault(emptyList()),
                    recentImports = recent.getOrDefault(emptyList())
                )
            }
        }
    }

    fun confirmImport(import: BankNotificationImport) {
        viewModelScope.launch {
            val result = confirmUseCase.confirm(import)
            if (result.isSuccess) {
                _uiState.update { it.copy(successMessage = "Lançamento registrado com sucesso!") }
                loadImports()
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun ignoreImport(importId: String) {
        viewModelScope.launch {
            confirmUseCase.ignore(importId)
            loadImports()
        }
    }

    fun toggleAutoMode() {
        viewModelScope.launch {
            appPrefs.setAutoMode(!autoMode.value)
        }
    }

    fun togglePackage(packageName: String) {
        viewModelScope.launch {
            appPrefs.togglePackage(packageName)
        }
    }

    fun checkNotificationPermission(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        val component = ComponentName(context, BankNotificationListenerService::class.java)
            .flattenToString()
        val granted = flat?.contains(component) == true
        _uiState.update { it.copy(notificationPermission = granted, serviceConnected = BankNotificationListenerService.isServiceRunning) }
        return granted
    }

    fun testParser(text: String): String {
        val result = NotificationParser.parse("test.package", "Teste", text, System.currentTimeMillis())
        return if (result.shouldIgnore) {
            "IGNORADO\nMotivo: ${result.ignoreReason}"
        } else {
            buildString {
                appendLine("TIPO: ${result.type.name}")
                appendLine("CATEGORIA: ${result.category.name}")
                appendLine("VALOR: R$ ${"%.2f".format(result.amount)}")
                appendLine("CONFIANÇA: ${result.confidenceScore}%")
                appendLine("DESCRIÇÃO: ${result.description}")
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
