package com.example.ppu_wt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class MiLugar(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lon: Double
)

class FavoritosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var listViewFavoritos: ListView
    private val listaLugares = mutableListOf<MiLugar>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favoritos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnVolver = findViewById<FloatingActionButton>(R.id.btnVolver)
        btnVolver.setOnClickListener { finish() }

        listViewFavoritos = findViewById(R.id.listaFavoritos)
        cargarFavoritosDesdeFirebase()

        // Eliminamos el setOnItemClickListener de aquí porque el Adaptador se hará cargo
    }

    private fun cargarFavoritosDesdeFirebase() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            finish()
            return
        }

        db.collection("usuarios").document(usuarioId).collection("favoritos")
            .get()
            .addOnSuccessListener { documentos ->
                listaLugares.clear()

                for (documento in documentos) {
                    val id = documento.id
                    val nombre = documento.getString("nombre") ?: "Sin nombre"
                    val lat = documento.getDouble("latitud") ?: 0.0
                    val lon = documento.getDouble("longitud") ?: 0.0

                    listaLugares.add(MiLugar(id, nombre, lat, lon))
                }

                if (listaLugares.isEmpty()) {
                    Toast.makeText(this, "Aún no tienes favoritos", Toast.LENGTH_SHORT).show()
                }

                val adaptador = FavoritosAdapter(this, listaLugares)
                listViewFavoritos.adapter = adaptador
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar la lista", Toast.LENGTH_SHORT).show()
            }
    }

    // =====================================================================
    // ADAPTADOR: AQUÍ REPARAMOS EL CLIC PARA VIAJAR AL MAPA
    // =====================================================================
    inner class FavoritosAdapter(context: Context, private val lugares: List<MiLugar>) :
        ArrayAdapter<MiLugar>(context, 0, lugares) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_favorito, parent, false)
            val lugar = lugares[position]

            val tvNombre = view.findViewById<TextView>(R.id.tvNombreLugar)
            val btnCompartir = view.findViewById<ImageButton>(R.id.btnCompartir)
            val btnEliminar = view.findViewById<ImageButton>(R.id.btnEliminar)

            // Atrapamos el contenedor principal de la fila
            val contenedorClickeable = view.findViewById<LinearLayout>(R.id.contenedorUbicacion)

            tvNombre.text = lugar.nombre

            // 1. VIAJAR AL MAPA (Solución al Bug de clic)
            contenedorClickeable.setOnClickListener {
                val intent = Intent(context, HomeActivity::class.java)
                intent.putExtra("viajar_lat", lugar.lat)
                intent.putExtra("viajar_lon", lugar.lon)
                intent.putExtra("viajar_nombre", lugar.nombre)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                context.startActivity(intent)
            }

            // 2. COMPARTIR
            btnCompartir.setOnClickListener {
                val urlMapa = "https://www.google.com/maps/search/?api=1&query=${lugar.lat},${lugar.lon}"
                val mensaje = "¡Mira mi lugar guardado en PPU Weather!\n📍 ${lugar.nombre}\n$urlMapa"

                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, mensaje)
                context.startActivity(Intent.createChooser(shareIntent, "Compartir ubicación vía:"))
            }

            // 3. ELIMINAR
            btnEliminar.setOnClickListener {
                val usuarioId = auth.currentUser?.uid ?: return@setOnClickListener

                AlertDialog.Builder(context)
                    .setTitle("Eliminar Favorito")
                    .setMessage("¿Estás seguro de borrar '${lugar.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        db.collection("usuarios").document(usuarioId)
                            .collection("favoritos").document(lugar.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Lugar eliminado", Toast.LENGTH_SHORT).show()
                                cargarFavoritosDesdeFirebase()
                            }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            return view
        }
    }
}