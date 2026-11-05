package com.example.aihub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Если приложения нет на телефоне: выбор, установить из Google Play или открыть официальный сайт.
 * Для веб-приложения: открыть сайт, чтобы установить его из меню браузера.
 */
class ChooserActivity : Activity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(L.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = ConfigStore.load(this)
        val svc = cfg.service(intent.getStringExtra("service") ?: "")
        if (svc == null) {
            finish()
            return
        }
        val browser = intent.getStringExtra("browser") ?: ""
        val url = intent.getStringExtra("url") ?: ""
        val kind = intent.getStringExtra("kind") ?: "app"
        val builder = Ui.dialog(this)
        if (kind == "pwa") {
            builder.setTitle(getString(R.string.chooser_pwa_title, svc.title))
                .setMessage(R.string.chooser_pwa_msg)
                .setPositiveButton(R.string.chooser_open_site) { _, _ ->
                    openSite(svc, browser, url)
                    finish()
                }
                .setNeutralButton(R.string.cancel) { _, _ -> finish() }
        } else {
            builder.setTitle(getString(R.string.chooser_title, svc.title))
                .setMessage(R.string.chooser_msg)
                .setPositiveButton(R.string.chooser_play) { _, _ ->
                    openPlay(svc)
                    finish()
                }
                .setNegativeButton(R.string.chooser_site) { _, _ ->
                    openSite(svc, browser, url)
                    finish()
                }
                .setNeutralButton(R.string.cancel) { _, _ -> finish() }
        }
        builder.setOnCancelListener { finish() }.show()
    }

    private fun openPlay(svc: ServiceCfg) {
        val playId = Catalog.byId(svc.id)?.playId ?: ""
        val name = Uri.encode(svc.title)
        val market = if (playId.isNotEmpty()) {
            "market://details?id=" + playId
        } else {
            "market://search?q=" + name + "&c=apps"
        }
        val web = if (playId.isNotEmpty()) {
            "https://play.google.com/store/apps/details?id=" + playId
        } else {
            "https://play.google.com/store/search?q=" + name + "&c=apps"
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(market)).setPackage("com.android.vending"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
            } catch (e2: Exception) {
                Toast.makeText(this, R.string.play_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSite(svc: ServiceCfg, browser: String, url: String) {
        val target = if (url.isNotEmpty()) url else svc.url
        try {
            startActivity(Browsers.siteIntent(this, target, browser))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
