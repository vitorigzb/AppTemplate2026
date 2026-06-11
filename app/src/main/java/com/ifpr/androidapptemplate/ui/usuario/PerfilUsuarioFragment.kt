package com.ifpr.androidapptemplate.ui.usuario

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Usuario

class PerfilUsuarioFragment : Fragment() {

    private lateinit var userProfileImageView: ImageView
    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerEnderecoEditText: EditText
    private lateinit var registerTelefoneEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button
    private lateinit var usersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var imageUri: Uri? = null
    private var fotoBase64Salva: String? = null

    // Abre a galeria e carrega a imagem em formato circular usando Glide
    private val pegarImagemDaGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop() // Corta a imagem em formato de círculo
                .placeholder(R.mipmap.ic_default_user)
                .error(R.mipmap.ic_default_user)
                .into(userProfileImageView)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_perfil_usuario, container, false)

        auth = FirebaseAuth.getInstance()

        userProfileImageView = view.findViewById(R.id.userProfileImageView)
        registerNameEditText = view.findViewById(R.id.registerNameEditText)
        registerEmailEditText = view.findViewById(R.id.registerEmailEditText)
        registerEnderecoEditText = view.findViewById(R.id.registerEnderecoEditText)
        registerTelefoneEditText = view.findViewById(R.id.registerTelefoneEditText)
        registerPasswordEditText = view.findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = view.findViewById(R.id.registerConfirmPasswordEditText)
        registerButton = view.findViewById(R.id.salvarButton)
        sairButton = view.findViewById(R.id.sairButton)

        try {
            usersReference = FirebaseDatabase.getInstance().getReference("users")
        } catch (e: Exception) {
            Log.e("DatabaseReference", "Erro ao obter referência para o Firebase", e)
        }

        val user = auth.currentUser

        if (user != null) {
            sairButton.visibility = View.VISIBLE
            registerPasswordEditText.visibility = View.GONE
            registerConfirmPasswordEditText.visibility = View.GONE
            registerEmailEditText.isEnabled = false

            registerNameEditText.setText(user.displayName)
            registerEmailEditText.setText(user.email)
            recuperarDadosUsuario(user.uid)
        }

        // Clique na imagem para trocar de foto
        userProfileImageView.setOnClickListener {
            pegarImagemDaGaleria.launch("image/*")
        }

        registerButton.setOnClickListener {
            updateUser()
        }

        sairButton.setOnClickListener {
            signOut()
        }

        return view
    }

    private fun signOut() {
        auth.signOut()
        Toast.makeText(context, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
        requireActivity().finish()
    }

    fun recuperarDadosUsuario(usuarioKey: String) {
        usersReference.child(usuarioKey).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val usuario = snapshot.getValue(Usuario::class.java)
                    usuario?.let {
                        registerEnderecoEditText.setText(it.endereco ?: "")
                        registerTelefoneEditText.setText(it.telefone ?: "")

                        // Se tiver foto em Base64, decodifica e renderiza circular
                        if (!it.foto.isNullOrEmpty()) {
                            fotoBase64Salva = it.foto
                            val imageBytes = Base64.decode(it.foto, Base64.DEFAULT)
                            Glide.with(this@PerfilUsuarioFragment)
                                .load(imageBytes)
                                .circleCrop() // Mantém o padrão circular ao carregar do Firebase
                                .placeholder(R.mipmap.ic_default_user)
                                .error(R.mipmap.ic_default_user)
                                .into(userProfileImageView)
                        } else {
                            userProfileImageView.setImageResource(R.mipmap.ic_default_user)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Erro ao recuperar dados: ${error.message}")
            }
        })
    }

    private fun updateUser() {
        val name = registerNameEditText.text.toString().trim()
        val endereco = registerEnderecoEditText.text.toString().trim()
        val telefone = registerTelefoneEditText.text.toString().trim()

        val user = auth.currentUser

        if (user != null) {
            var stringFotoBase64 = fotoBase64Salva

            if (imageUri != null) {
                try {
                    val inputStream = context?.contentResolver?.openInputStream(imageUri!!)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        stringFotoBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    }
                } catch (e: Exception) {
                    Log.e("PerfilUsuario", "Erro ao converter imagem", e)
                }
            }

            updateProfile(user, name, endereco, telefone, stringFotoBase64)
        } else {
            Toast.makeText(context, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfile(user: FirebaseUser, displayName: String, endereco: String, telefone: String, fotoBase64: String?) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        val usuario = Usuario(user.uid, displayName, user.email, endereco, telefone, fotoBase64)

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase(usuario)
                } else {
                    Toast.makeText(context, "Erro ao atualizar o perfil de autenticação.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase(usuario: Usuario) {
        usuario.key?.let { id ->
            usersReference.child(id).setValue(usuario)
                .addOnSuccessListener {
                    Toast.makeText(context, "Usuário atualizado com sucesso! ✨", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Falha ao salvar no banco de dados.", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(context, "ID inválido", Toast.LENGTH_SHORT).show()
        }
    }
}