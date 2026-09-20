package com.example.ppu_wt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    // Variable global para manejar Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ENLAZAR VISTAS (Asegúrate de que estos ID coincidan con tu activity_main.xml) ---
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etContrasena = findViewById<EditText>(R.id.etPassword) // Reemplaza password_et por tu ID si es diferente
        val btnLogin = findViewById<Button>(R.id.btnIniciar)
        val btnRegistro = findViewById<Button>(R.id.btnRegistrar)
        val btnInvitado = findViewById<Button>(R.id.btnInvitado) // El que arreglamos antes

        // --- 1. LÓGICA DEL BOTÓN REGISTRO ---
        btnRegistro.setOnClickListener {
            val correo = etCorreo.text.toString()
            val contrasena = etContrasena.text.toString()

            if (correo.isNotEmpty() && contrasena.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Registro Exitoso", Toast.LENGTH_SHORT).show()
                            irAlHome()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // --- 2. LÓGICA DEL BOTÓN LOGIN ---
        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString()
            val contrasena = etContrasena.text.toString()

            if (correo.isNotEmpty() && contrasena.isNotEmpty()) {
                auth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            irAlHome()
                        } else {
                            Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // --- 3. LÓGICA DEL BOTÓN INVITADO (Mantenemos lo que ya funcionaba) ---
        btnInvitado.setOnClickListener {
            auth.signOut() // ¡Cierra cualquier cuenta que haya quedado abierta!
            irAlHome()
        }
    }

    // Función auxiliar para no repetir código
    private fun irAlHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}