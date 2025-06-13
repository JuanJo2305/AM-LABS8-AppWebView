package com.example.perufood

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AlertDialog


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fab: FloatingActionButton
    private var menuItemFavorito: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de usar tu tema antes de inflar
        setTheme(R.style.Theme_PeruFood)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webView)
        fab = findViewById(R.id.fabOptions)

        // Configuración WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true                  // Activa DOM storage
            allowFileAccess = true
            allowContentAccess = true
            // User‑Agent personalizado
            val defaultUA = userAgentString
            userAgentString = "$defaultUA Mozilla/5.0 (Android)"
        }

        // Configuración de cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }

        // Manejo de errores y navegación
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                menuItemFavorito?.let { actualizarIconoFavorito(it) }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                Toast.makeText(this@MainActivity,
                    "Error ${error.errorCode}: ${error.description}",
                    Toast.LENGTH_LONG).show()
            }
        }


        // Cargar la URL inicial
        webView.loadUrl("https://www.recetasgratis.net/recetas-peruanas")

        // Registro para menú contextual
        registerForContextMenu(webView)

        // Botón de PopupMenu
        fab.setOnClickListener { view ->
            android.widget.PopupMenu(this, view).apply {
                menuInflater.inflate(R.menu.popup_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.popup_reload -> webView.reload()
                        R.id.popup_home -> webView.loadUrl("https://www.recetasgratis.net/recetas-peruanas")
                    }
                    true
                }
            }.show()
        }
    }

    override fun onCreateContextMenu(
        menu: android.view.ContextMenu?,
        v: android.view.View?,
        menuInfo: android.view.ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.menu_context, menu)
    }

    override fun onContextItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.context_share -> {
                webView.url?.let { url ->
                    startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                    )
                }
            }
            R.id.context_copy -> {
                webView.url?.let { url ->
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("URL", url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "URL copiada", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onBackPressed() {
        // Navegar atrás en WebView si es posible
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        menuItemFavorito = menu?.findItem(R.id.menu_favoritos)
        menuItemFavorito?.let { actualizarIconoFavorito(it) }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_acerca -> {
                AlertDialog.Builder(this)
                    .setTitle("Acerca de PeruFood")
                    .setMessage("Aplicación Web View de recetas de comida peruana.\nDesarrollado por Juanjo.")
                    .setPositiveButton("Cerrar", null)
                    .show()

                webView.loadUrl("https://www.recetasgratis.net/quienes-somos")
                return true
            }

            R.id.menu_favoritos -> {
                toggleFavorito()
                actualizarIconoFavorito(item)
                return true
            }

            R.id.menu_ver_favoritos -> {
                mostrarListaFavoritos()
                return true
            }
        }
        return super.onOptionsItemSelected(item)

    }

    private fun actualizarIconoFavorito(menuItem: MenuItem) {
        val currentUrl = webView.url ?: return
        val prefs = getSharedPreferences("favoritos", MODE_PRIVATE)
        val favoritosJson = prefs.getString("favoritos", "{}")
        val favoritosMap = org.json.JSONObject(favoritosJson ?: "{}")

        val esFavorito = favoritosMap.has(currentUrl)
        menuItem.setIcon(
            if (esFavorito)
                R.drawable.baseline_favorite_24
            else
                R.drawable.baseline_favorite_border_24
        )
    }


    private fun toggleFavorito() {
        val currentUrl = webView.url ?: return
        val currentTitle = webView.title ?: "Favorito sin título"
        val prefs = getSharedPreferences("favoritos", MODE_PRIVATE)
        val editor = prefs.edit()

        val favoritosJson = prefs.getString("favoritos", "{}")
        val favoritosMap = org.json.JSONObject(favoritosJson ?: "{}")

        if (favoritosMap.has(currentUrl)) {
            favoritosMap.remove(currentUrl)
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
        } else {
            favoritosMap.put(currentUrl, currentTitle)
            Toast.makeText(this, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
        }

        editor.putString("favoritos", favoritosMap.toString())
        editor.apply()
    }

    private fun mostrarListaFavoritos() {
        val prefs = getSharedPreferences("favoritos", MODE_PRIVATE)
        val favoritosJson = prefs.getString("favoritos", "{}")
        val favoritosMap = org.json.JSONObject(favoritosJson ?: "{}")

        if (favoritosMap.length() == 0) {
            Toast.makeText(this, "No tienes favoritos guardados.", Toast.LENGTH_SHORT).show()
            return
        }

        val urls = favoritosMap.keys().asSequence().toList()
        val titulos = urls.map { favoritosMap.getString(it) }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tus Favoritos")
        builder.setItems(titulos.toTypedArray()) { _, which ->
            webView.loadUrl(urls[which])
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }






}
