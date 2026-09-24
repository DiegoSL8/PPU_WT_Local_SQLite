package com.example.ppu_wt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private lateinit var mapa: GoogleMap
    private var marcadorActual: Marker? = null
    private var ubicacionSeleccionada: LatLng? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var panelLateral: CardView
    private lateinit var vistaExpandida: LinearLayout

    private lateinit var tvTempCompacta: TextView
    private lateinit var tvVientoExpandido: TextView
    private lateinit var tvUvExpandido: TextView
    private lateinit var tvAptoBanio: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        configurarInterfaz()
        prepararMapa()
        configurarBuscador()
        configurarBotonesMapa()
    }

    private fun configurarInterfaz() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapaFondo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        panelLateral = findViewById(R.id.panelLateral)
        vistaExpandida = findViewById(R.id.vistaExpandida)

        tvTempCompacta = findViewById(R.id.tvTempCompacta)
        tvVientoExpandido = findViewById(R.id.tvVientoExpandido)
        tvUvExpandido = findViewById(R.id.tvUvExpandido)
        tvAptoBanio = findViewById(R.id.tvAptoBanio)

        val btnIrFavoritos = findViewById<Button>(R.id.btnIrFavoritos)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        val btnGuardarPin = findViewById<FloatingActionButton>(R.id.btnGuardarPin)

        // 1. Expansión del panel
        panelLateral.setOnClickListener {
            vistaExpandida.visibility = if (vistaExpandida.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // 2. Cerrar Sesión seguro
        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // 3. Protección de Favoritos para Invitados
        btnIrFavoritos.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Debes registrarte para ver tus favoritos", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, FavoritosActivity::class.java))
            }
        }

        // 4. Guardar Pin (Con reglas de negocio)
        btnGuardarPin.setOnClickListener { guardarLugarFavorito() }
    }

    private fun configurarBuscador() {
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        etBuscar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                buscarUbicacionEnMapa(etBuscar.text.toString())
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etBuscar.windowToken, 0)
                true
            } else false
        }
    }

    private fun configurarBotonesMapa() {
        findViewById<FloatingActionButton>(R.id.btnMiUbicacion).setOnClickListener { centrarEnUbicacionUsuario() }
        findViewById<FloatingActionButton>(R.id.btnZoomIn).setOnClickListener { mapa.animateCamera(CameraUpdateFactory.zoomIn()) }
        findViewById<FloatingActionButton>(R.id.btnZoomOut).setOnClickListener { mapa.animateCamera(CameraUpdateFactory.zoomOut()) }
    }

    private fun prepararMapa() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapaFondo) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        mapa.setOnMapClickListener(this)

        // 1. Revisamos si el usuario acaba de llegar desde la lista de Favoritos
        val viajarLat = intent.getDoubleExtra("viajar_lat", 0.0)
        val viajarLon = intent.getDoubleExtra("viajar_lon", 0.0)
        val viajarNombre = intent.getStringExtra("viajar_nombre")

        if (viajarLat != 0.0 && viajarLon != 0.0) {
            // Llegamos desde favoritos: Viajamos directo a ese punto
            val coords = LatLng(viajarLat, viajarLon)
            colocarMarcador(coords, viajarNombre ?: "Favorito")
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 14f))
            ubicacionSeleccionada = coords
            obtenerClima(viajarLat, viajarLon)

            // Habilitar el GPS sin forzar el centrado
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mapa.isMyLocationEnabled = true
            }
        } else {
            // 2. Flujo normal al abrir la app: Centrar en el GPS del usuario
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mapa.isMyLocationEnabled = true
                centrarEnUbicacionUsuario()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun centrarEnUbicacionUsuario() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val pos = if (location != null) LatLng(location.latitude, location.longitude) else LatLng(-20.2133, -70.1503)
            ubicacionSeleccionada = pos
            colocarMarcador(pos, "Mi Ubicación")
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            obtenerClima(pos.latitude, pos.longitude)
        }
    }

    override fun onMapClick(coordenadas: LatLng) {
        colocarMarcador(coordenadas, "Pin Manual")
        ubicacionSeleccionada = coordenadas
        obtenerClima(coordenadas.latitude, coordenadas.longitude)
    }

    private fun colocarMarcador(coordenadas: LatLng, titulo: String) {
        marcadorActual?.remove()
        marcadorActual = mapa.addMarker(MarkerOptions().position(coordenadas).title(titulo))
    }

    private fun buscarUbicacionEnMapa(nombre: String) {
        if (nombre.trim().isEmpty()) return
        try {
            val direcciones = Geocoder(this, Locale.getDefault()).getFromLocationName(nombre, 1)
            if (!direcciones.isNullOrEmpty()) {
                val coords = LatLng(direcciones[0].latitude, direcciones[0].longitude)
                mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 12f))
                colocarMarcador(coords, nombre)
                ubicacionSeleccionada = coords
                obtenerClima(coords.latitude, coords.longitude)
            } else Toast.makeText(this, "Lugar no encontrado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error de búsqueda", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerClima(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder().baseUrl("https://api.open-meteo.com/").addConverterFactory(GsonConverterFactory.create()).build()
        retrofit.create(WeatherApiService::class.java).obtenerClimaActual(lat, lon).enqueue(object : Callback<ClimaResponse> {
            override fun onResponse(call: Call<ClimaResponse>, response: Response<ClimaResponse>) {
                val clima = response.body()?.actual
                if (clima != null) {
                    tvTempCompacta.text = "${clima.temperatura}°C"
                    tvVientoExpandido.text = "🌬️ Viento: ${clima.viento} km/h"
                    tvUvExpandido.text = "☀️ Índice UV: ${if (clima.esDeDia == 1) "Alto (8)" else "Bajo (0)"}"

                    val esApto = if (clima.viento > 15.0) "No (Bandera Roja)" else "Sí (Bandera Verde)"
                    tvAptoBanio.text = "🌊 Apto para Baño: $esApto"
                    tvAptoBanio.setTextColor(if (clima.viento > 15.0) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32"))
                }
            }
            override fun onFailure(call: Call<ClimaResponse>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Fallo al obtener clima", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarLugarFavorito() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "El modo invitado no puede guardar pines", Toast.LENGTH_SHORT).show()
            return
        }
        if (ubicacionSeleccionada == null) {
            Toast.makeText(this, "Selecciona un punto en el mapa primero", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply { hint = "Ej: Casa, Playa Cavancha..." }
        AlertDialog.Builder(this)
            .setTitle("Guardar Ubicación")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nombreIngresado = input.text.toString().trim()

                // Validación Estricta: Si está vacío, alerta y no guarda
                if (nombreIngresado.isEmpty()) {
                    Toast.makeText(this, "Error: Debes ponerle un nombre para guardar", Toast.LENGTH_LONG).show()
                } else {
                    val nuevo = LugarFavorito("", nombreIngresado, ubicacionSeleccionada!!.latitude, ubicacionSeleccionada!!.longitude)
                    db.collection("usuarios").document(usuarioId).collection("favoritos").add(nuevo)
                        .addOnSuccessListener { Toast.makeText(this, "¡Lugar guardado!", Toast.LENGTH_SHORT).show() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }
    // Se ejecuta si HomeActivity ya estaba abierta de fondo y volvemos a ella
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val viajarLat = intent.getDoubleExtra("viajar_lat", 0.0)
        val viajarLon = intent.getDoubleExtra("viajar_lon", 0.0)
        val viajarNombre = intent.getStringExtra("viajar_nombre")

        if (viajarLat != 0.0 && viajarLon != 0.0) {
            val coords = LatLng(viajarLat, viajarLon)
            colocarMarcador(coords, viajarNombre ?: "Favorito")
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 14f))
            ubicacionSeleccionada = coords
            obtenerClima(viajarLat, viajarLon)
        }
    }

}