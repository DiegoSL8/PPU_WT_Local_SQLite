package com.example.ppu_wt.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ppu_wt.R
import com.example.ppu_wt.data.LugaresRepository
import com.example.ppu_wt.model.LugarFavoritoSql
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FavoritosActivity : AppCompatActivity() {

    // --- DEPENDENCIAS LOCALES (Sustituyen a Firebase) ---
    private lateinit var repositorio: LugaresRepository
    private lateinit var sharedPreferences: SharedPreferences

    // --- VISTAS Y DATOS ---
    private lateinit var listViewFavoritos: ListView
    private var listaLugaresFavoritos = mutableListOf<LugarFavoritoSql>()
    private var idUsuarioActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favoritos)

        // 1. Inicialización de la sesión local y el repositorio SQLite
        sharedPreferences = getSharedPreferences("sesion_ppu_wt", Context.MODE_PRIVATE)
        repositorio = LugaresRepository(this)

        // 2. Extracción del ID de usuario
        idUsuarioActual = sharedPreferences.getString("usuario_id", "") ?: ""

        // 3. REGLA DE NEGOCIO: Validación de Modo Invitado
        if (idUsuarioActual.isEmpty()) {
            Toast.makeText(this, "Modo Invitado: No tienes acceso a favoritos.", Toast.LENGTH_LONG).show()
            finish() // Cierra la actividad inmediatamente y devuelve al Home
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnVolver = findViewById<FloatingActionButton>(R.id.btnVolver)
        btnVolver.setOnClickListener { finish() }

        listViewFavoritos = findViewById(R.id.listaFavoritos)

        // 4. Lectura desde SQLite
        cargarFavoritosDesdeSQLite()
    }

    /**
     * READ: Consulta todos los favoritos de este usuario desde SQLite y actualiza la UI.
     */
    private fun cargarFavoritosDesdeSQLite() {
        listaLugaresFavoritos.clear()

        // Obtenemos los lugares usando el ID del usuario en sesión
        val favoritosObtenidos = repositorio.obtenerFavoritos(idUsuarioActual)
        listaLugaresFavoritos.addAll(favoritosObtenidos)

        if (listaLugaresFavoritos.isEmpty()) {
            Toast.makeText(this, "Aún no tienes lugares favoritos", Toast.LENGTH_SHORT).show()
        }

        val adaptador = FavoritosAdapter(this, listaLugaresFavoritos)
        listViewFavoritos.adapter = adaptador
    }

    // =====================================================================
    // ADAPTADOR PERSONALIZADO PARA LA LISTA (Interactividad por ítem)
    // =====================================================================
    inner class FavoritosAdapter(context: Context, private val lugares: List<LugarFavoritoSql>) :
        ArrayAdapter<LugarFavoritoSql>(context, 0, lugares) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_favorito, parent, false)
            val lugar = lugares[position]

            val tvNombre = view.findViewById<TextView>(R.id.tvNombreLugar)
            val btnCompartir = view.findViewById<ImageButton>(R.id.btnCompartir)
            val btnEliminar = view.findViewById<ImageButton>(R.id.btnEliminar)
            val btnEditar = view.findViewById<ImageButton>(R.id.btnEditar)
            val contenedorClickeable = view.findViewById<View>(R.id.contenedorUbicacion)

            tvNombre.text = lugar.nombre

            // 1. VIAJAR AL MAPA (Clic en toda la fila)
            contenedorClickeable.setOnClickListener {
                val intent = Intent(this@FavoritosActivity, HomeActivity::class.java)
                intent.putExtra("viajar_lat", lugar.latitud)
                intent.putExtra("viajar_lon", lugar.longitud)
                intent.putExtra("viajar_nombre", lugar.nombre)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@FavoritosActivity.startActivity(intent)
            }

            // 2. ACTUALIZAR (UPDATE en SQLite) - Clic simple en el botón Editar
            btnEditar.setOnClickListener {
                val input = EditText(this@FavoritosActivity).apply {
                    setText(lugar.nombre) // Mostrar el nombre actual
                }

                AlertDialog.Builder(this@FavoritosActivity)
                    .setTitle("Editar Favorito")
                    .setMessage("Ingresa un nuevo nombre para esta ubicación:")
                    .setView(input)
                    .setPositiveButton("Actualizar") { dialog, _ ->
                        val nuevoNombre = input.text.toString().trim()
                        if (nuevoNombre.isNotEmpty()) {
                            // Llamamos a la función de actualizar en el repositorio
                            val actualizado = repositorio.actualizarNombreFavorito(lugar.id, nuevoNombre)
                            if (actualizado) {
                                Toast.makeText(this@FavoritosActivity, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                                cargarFavoritosDesdeSQLite() // Recargar la lista
                            } else {
                                Toast.makeText(this@FavoritosActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@FavoritosActivity, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
                    .show()
            }

            // 3. COMPARTIR
            btnCompartir.setOnClickListener {
                val urlMapa = "https://www.google.com/maps/search/?api=1&query=${lugar.latitud},${lugar.longitud}"
                val mensaje = "¡Mira mi lugar favorito!\n📍 ${lugar.nombre}\n$urlMapa"

                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, mensaje)
                this@FavoritosActivity.startActivity(Intent.createChooser(shareIntent, "Compartir ubicación vía:"))
            }

            // 4. ELIMINAR (DELETE en SQLite)
            btnEliminar.setOnClickListener {
                AlertDialog.Builder(this@FavoritosActivity)
                    .setTitle("Eliminar Favorito")
                    .setMessage("¿Estás seguro de borrar '${lugar.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->

                        val eliminado = repositorio.eliminarFavorito(lugar.id)
                        if(eliminado){
                            Toast.makeText(this@FavoritosActivity, "Lugar eliminado", Toast.LENGTH_SHORT).show()
                            cargarFavoritosDesdeSQLite() // Refrescamos la lista
                        }else{
                            Toast.makeText(this@FavoritosActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            return view
        }
    }
}