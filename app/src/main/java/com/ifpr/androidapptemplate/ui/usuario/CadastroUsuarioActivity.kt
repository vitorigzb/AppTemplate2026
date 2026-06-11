package com.ifpr.androidapptemplate.ui.usuario

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.ifpr.androidapptemplate.databinding.ActivityCadastroUsuarioBinding

class CadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroUsuarioBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o View Binding
        binding = ActivityCadastroUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configurações dos botões de ação
        binding.salvarButton.setOnClickListener {
            createAccount()
        }

        binding.sairButton.setOnClickListener {
            finish()
        }
    }

    private fun createAccount() {
        val name = binding.registerNameEditText.text.toString().trim()
        val email = binding.registerEmailEditText.text.toString().trim()
        val password = binding.registerPasswordEditText.text.toString().trim()
        val confirmPassword = binding.registerConfirmPasswordEditText.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Novo usuário cadastrado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val user = auth.currentUser
                        updateProfile(user, name)
                        sendEmailVerification(user)
                    } else {
                        val errorMessage = task.exception?.message ?: "Erro desconhecido"
                        Log.e("FirebaseAuth", "Erro ao cadastrar usuário: $errorMessage")
                        Toast.makeText(
                            this,
                            "Falha ao cadastrar novo usuário: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirebaseAuth", "Erro ao conectar com o Firebase", ex)
            Toast.makeText(
                this,
                "Falha ao conectar com o Firebase: ${ex.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun sendEmailVerification(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        "E-mail de verificação enviado para ${user.email}.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Falha ao enviar e-mail de verificação.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateProfile(user: FirebaseUser?, displayName: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Nome do usuário atualizado no perfil do Firebase.")
                } else {
                    Log.e("FirebaseAuth", "Não foi possível atualizar o nome de exibição.")
                }
            }
    }
}