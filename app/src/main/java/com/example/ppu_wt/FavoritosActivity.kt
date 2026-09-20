package com.example.ppu_wt

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var listViewFavoritos: ListView // Lista temporal para mostrar los datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favoritos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnVolver = findViewById<Button>(R.id.btnVolver)
        btnVolver.setOnClickListener { finish() }

        // Aquí deberías tener un ListView o LinearLayout en tu activity_favoritos.xml
        // Por ahora, asumiremos que tienes un ListView con el id "listaFavoritos"
        // Si no lo tienes, la app no se caerá, pero te avisará
        try {
            listViewFavoritos = findViewById(R.id.listaFavoritos)
            cargarFavoritosDesdeFirebase()
        } catch (e: Exception) {
            Toast.makeText(this, "Falta agregar un ListView en el diseño XML", Toast.LENGTH_LONG).show()
        }
    }

    private fun cargarFavoritosDesdeFirebase() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Inicia sesión para ver tus favoritos", Toast.LENGTH_SHORT).show()
            return
        }

        // Descargamos la lista desde Firestore
        db.collection("usuarios").document(usuarioId).collection("favoritos")
            .get()
            .addOnSuccessListener { documentos ->
                val nombresLugares = mutableListOf<String>()

                for (documento in documentos) {
                    // Leemos el nombre que le pusiste al pin
                    val nombre = documento.getString("nombre") ?: "Sin nombre"
                    nombresLugares.add(nombre)
                }

                // Si la lista está vacía
                if (nombresLugares.isEmpty()) {
                    nombresLugares.add("Aún no tienes lugares favoritos")
                }

                // Metemos los nombres en la vista de lista de Android
                val adaptador = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresLugares)
                listViewFavoritos.adapter = adaptador
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
            }
    }
}