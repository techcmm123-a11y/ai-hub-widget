package com.example.aihub

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast

/** Невидимый экран: по нажатию кнопки виджета свежо проверяет приложения и открывает нужное. */
class LaunchActivity : Activity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(L.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Apps.refresh()
            val cfg = ConfigStore.load(this)
            val svc = cfg.service(intent.getStringExtra("service") ?: "")
            val entryId = intent.getStringExtra("entry") ?: ""
            val entry = cfg.entries.firstOrNull { it.id == entryId }
            if (svc != null && entry != null) {
                startActivity(Open.resolveNow(this, svc, entry))
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
