package com.example.ppu_wt.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ppu_wt.ui.HomeActivity
import com.example.ppu_wt.R
import com.example.ppu_wt.utils.SecurityUtils

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Inicialización de la sesión local
        sharedPreferences = getSharedPreferences("sesion_ppu_wt", MODE_PRIVATE)

        // 2. Comprobación de sesión activa (evita que el usuario deba loguearse cada vez)
        val sesionActiva = sharedPreferences.getString("usuario_id", "") ?: ""
        if (sesionActiva.isNotEmpty()) {
            irAlHome()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Enlace de vistas
        val etCorreo = findViewById<EditText>(R.id.email_et)
        val etContrasena = findViewById<EditText>(R.id.password_et)
        val btnLogin = findViewById<Button>(R.id.login_btn)
        val btnRegistro = findViewById<Button>(R.id.register_btn)
        val btnInvitado = findViewById<Button>(R.id.btnInvitado)

        // 4. Lógica de Registro
        btnRegistro.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (validarCredenciales(correo, contrasena)) {
                guardarSesionLocal(correo)
                Toast.makeText(this, "Registro local exitoso", Toast.LENGTH_SHORT).show()
                irAlHome()
            }
        }

        // 5. Lógica de Login
        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val contrasenaProtegida = SecurityUtils.hashPassword(contrasena)

            if (validarCredenciales(correo, contrasena)) {
                guardarSesionLocal(correo)
                Toast.makeText(this, "Sesión iniciada: $correo", Toast.LENGTH_SHORT).show()
                irAlHome()
            }
        }

        // 6. Lógica de Invitado
        btnInvitado.setOnClickListener {
            // Limpiamos cualquier sesión previa para garantizar el anonimato
            sharedPreferences.edit().clear().apply()
            irAlHome()
        }
    }

    /**
     * Valida que el correo tenga el formato correcto y la contraseña sea segura.
     */
    private fun validarCredenciales(correo: String, contrasena: String): Boolean {
        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Debe completar todos los campos", Toast.LENGTH_SHORT).show()
            return false
        }

        // Valida el formato del correo usando la herramienta nativa de Android
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingrese un formato de correo válido", Toast.LENGTH_LONG).show()
            return false
        }

        // Valida que la contraseña tenga al menos 6 caracteres
        if (contrasena.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    /**
     * Guarda el ID del usuario en la memoria del dispositivo.
     */
    private fun guardarSesionLocal(correo: String) {
        sharedPreferences.edit()
            .putString("usuario_id", correo)
            .apply()
    }

    /**
     * Navega a la pantalla principal y limpia el historial de navegación.
     */
    private fun irAlHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Cierra MainActivity para que no se pueda volver atrás
    }
}