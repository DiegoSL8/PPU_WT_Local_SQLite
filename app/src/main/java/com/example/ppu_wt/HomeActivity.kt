package com.example.ppu_wt

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.app.AlertDialog

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    // Variables globales
    private lateinit var mapa: GoogleMap
    private var marcadorActual: Marker? = null // Guardará el Pin rojo
    private var ubicacionSeleccionada: LatLng? = null // Guardará las coordenadas

    // Variables para los textos de la pantalla
    private lateinit var tvTempAct: TextView
    private lateinit var tvVientoAct: TextView
    private lateinit var tvUvAct: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Enlazar las vistas
        tvTempAct = findViewById(R.id.tvTempAct)
        tvVientoAct = findViewById(R.id.tvVientoAct)
        tvUvAct = findViewById(R.id.tvUvAct)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapaFondo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Preparamos el mapa (movimos esto aquí para no tener dos onCreate)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapaFondo) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // --- NAVEGACIÓN ---
        val btnFavoritos = findViewById<ImageView>(R.id.btnFavorito)

        // Clic normal: Ir a la pantalla de la lista
        btnFavoritos.setOnClickListener {
            val intent = Intent(this, FavoritosActivity::class.java)
            startActivity(intent)
        }

        // Pulsación larga: Guardar en Firebase abriendo la ventana del nombre
        btnFavoritos.setOnLongClickListener {
            guardarLugarFavorito()
            true
        }

        // --- LLAMADA A LA API DE OPEN-METEO ---
        obtenerClimaIquique()
    }

    private fun obtenerClimaIquique() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApiService::class.java)

        val latIquique = -20.2133
        val lonIquique = -70.1503

        service.obtenerClimaActual(latIquique, lonIquique).enqueue(object : Callback<ClimaResponse> {
            override fun onResponse(call: Call<ClimaResponse>, response: Response<ClimaResponse>) {
                if (response.isSuccessful) {
                    val clima = response.body()?.actual
                    if (clima != null) {
                        tvTempAct.text = "${clima.temperatura}°C"
                        tvVientoAct.text = "Viento: ${clima.viento} km/h"
                        val esDia = if (clima.esDeDia == 1) "☀️ Día" else "🌙 Noche"
                        tvUvAct.text = esDia
                    }
                } else {
                    Toast.makeText(this@HomeActivity, "Error al leer datos del clima", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ClimaResponse>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Fallo la conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarLugarFavorito() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Validamos que haya puesto un pin en el mapa
        if (ubicacionSeleccionada == null) {
            Toast.makeText(this, "Primero toca el mapa para poner un marcador", Toast.LENGTH_SHORT).show()
            return
        }

        // Creamos una ventana emergente para pedir el nombre
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Favorito")

        val input = EditText(this)
        input.hint = "Ej: Casa Iquique, Playa Cavancha..."
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nombreLugar = input.text.toString()

            if (nombreLugar.isNotEmpty()) {
                // Guardamos en Firebase con el nombre y coordenadas reales
                val nuevoFavorito = LugarFavorito(
                    id = "",
                    nombre = nombreLugar,
                    latitud = ubicacionSeleccionada!!.latitude,
                    longitud = ubicacionSeleccionada!!.longitude
                )

                db.collection("usuarios").document(usuarioId)
                    .collection("favoritos")
                    .add(nuevoFavorito)
                    .addOnSuccessListener {
                        Toast.makeText(this, "¡$nombreLugar guardado!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Esta función se ejecuta sola cuando el mapa de Google está listo
    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        // Le decimos que escuche los clics del usuario
        mapa.setOnMapClickListener(this)

        // Por defecto, centramos la cámara en Iquique (Zoom 12f)
        val iquique = LatLng(-20.2133, -70.1503)
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(iquique, 12f))
    }

    // Esta función se ejecuta cada vez que el usuario toca el mapa
    override fun onMapClick(coordenadas: LatLng) {
        // Si ya había un pin, lo borramos
        marcadorActual?.remove()

        // Colocamos un Pin nuevo donde el usuario tocó
        marcadorActual = mapa.addMarker(MarkerOptions().position(coordenadas).title("Ubicación seleccionada"))

        // Guardamos las coordenadas en nuestra variable global
        ubicacionSeleccionada = coordenadas
    }
}