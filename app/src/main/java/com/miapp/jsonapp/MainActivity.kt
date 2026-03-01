package com.miapp.jsonapp

import android.app.Activity
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val REQUEST_SAVE = 1001
    private var jsonValidado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvEstado = findViewById<TextView>(R.id.tvEstado)
        val tvPreview = findViewById<TextView>(R.id.tvPreview)
        val btnPegar = findViewById<Button>(R.id.btnPegar)
        val btnExportar = findViewById<Button>(R.id.btnExportar)

        btnExportar.isEnabled = false

        btnPegar.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val texto = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()

            if (texto.isNullOrEmpty()) {
                tvEstado.text = "⚠️ El portapapeles está vacío"
                tvEstado.setTextColor(getColor(android.R.color.holo_orange_dark))
                tvPreview.text = ""
                btnExportar.isEnabled = false
                return@setOnClickListener
            }

            val resultado = validarJSON(texto)
            if (resultado != null) {
                jsonValidado = resultado
                tvEstado.text = "✅ JSON válido — listo para exportar"
                tvEstado.setTextColor(getColor(android.R.color.holo_green_dark))
                tvPreview.text = jsonValidado.take(500) + if (jsonValidado.length > 500) "\n..." else ""
                btnExportar.isEnabled = true
            } else {
                tvEstado.text = "❌ JSON inválido — revisa el contenido"
                tvEstado.setTextColor(getColor(android.R.color.holo_red_dark))
                tvPreview.text = texto.take(300)
                btnExportar.isEnabled = false
            }
        }

        btnExportar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                guardarConMediaStore()
            } else {
                abrirSelectorArchivo()
            }
        }
    }

    private fun validarJSON(texto: String): String? {
        return try {
            JSONObject(texto).toString(2)
        } catch (e: JSONException) {
            try {
                JSONArray(texto).toString(2)
            } catch (e2: JSONException) {
                null
            }
        }
    }

    private fun guardarConMediaStore() {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "datos.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonValidado.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            Toast.makeText(this, "✅ Guardado en Descargas/datos.json", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "❌ Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirSelectorArchivo() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "datos.json")
        }
        startActivityForResult(intent, REQUEST_SAVE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SAVE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri: Uri ->
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonValidado.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "✅ Archivo guardado correctamente", Toast.LENGTH_LONG).show()
            }
        }
    }
}
