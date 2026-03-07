package com.gastospro.app

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        var webViewRef: WebView? = null

        fun enviarLancamentoParaIndex(json: String) {
            webViewRef?.post {
                val script = "window.receberLancamentoBancario($json);"
                webViewRef?.evaluateJavascript(script, null)
            }
        }
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webViewRef = webView

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Se seu index está dentro do app:
        webView.loadUrl("file:///android_asset/index.html")

        // Se estiver hospedado:
        // webView.loadUrl("https://seusite.com/index.html")

        findViewById<Button>(R.id.btnPermissaoNotif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnTeste).setOnClickListener {
            val fake = JSONObject().apply {
                put("tipo", "entrada")
                put("valor", 150.0)
                put("descricao", "Pix recebido de João")
                put("banco", "Banco Pan")
                put("dataHora", System.currentTimeMillis().toString())
                put("hash", "teste_manual_001")
            }
            enviarLancamentoParaIndex(fake.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (webViewRef === webView) webViewRef = null
    }

    fun notificationAccessEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val expected = ComponentName(this, BankNotificationListener::class.java).flattenToString()
        return flat?.contains(expected) == true
    }
}