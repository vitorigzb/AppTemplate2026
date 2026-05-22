package com.ifpr.androidapptemplate

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.baseclasses.Item

class DetalhesItemActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhes_item)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val itemId = intent.getStringExtra("ITEM_ID")

        if (itemId != null) {
            buscarDetalhesDoItem(itemId)
        } else {
            Toast.makeText(this, "Erro: ID do item não encontrado.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun buscarDetalhesDoItem(itemId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val databaseRef = FirebaseDatabase.getInstance().getReference("itens")

        databaseRef.child(uid).child(itemId).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val itemEncontrado = snapshot.getValue(Item::class.java)

                if (itemEncontrado != null) {
                    preencherCamposDaTela(itemEncontrado)
                } else {
                    Toast.makeText(this@DetalhesItemActivity, "Item não encontrado nesta conta.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetalhesItemActivity, "Erro ao conectar ao banco.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun preencherCamposDaTela(item: Item) {
        // Vincula todos os novos componentes do XML
        val imageView = findViewById<ImageView>(R.id.imageViewDetalhe)
        val textViewEmoji = findViewById<TextView>(R.id.textViewEmojiDetalhe)
        val textViewTitulo = findViewById<TextView>(R.id.textViewTituloDetalhe)
        val textViewData = findViewById<TextView>(R.id.textViewDataDetalhe)
        val textViewDescricao = findViewById<TextView>(R.id.textViewDescricaoDetalhe)
        val textViewLocalizacao = findViewById<TextView>(R.id.textViewLocalizacaoDetalhe)
        val textViewIntensidade = findViewById<TextView>(R.id.textViewIntensidadeDetalhe)

        // Preenche cada um com os dados vindos do seu objeto Item
        textViewEmoji.text = item.emoji ?: "😐"
        textViewTitulo.text = item.categoria?.uppercase() ?: "SEM CATEGORIA"
        textViewData.text = item.data ?: ""
        textViewDescricao.text = item.descricao ?: "Sem descrição."
        textViewLocalizacao.text = item.localizacao ?: "Sem localização"

        val intensidade = item.intensidade ?: 0
        textViewIntensidade.text = "Intensidade: $intensidade"

        // Processa e exibe a imagem
        if (!item.foto.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(item.foto, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (_: Exception) {
                // Mantém imagem padrão caso dê erro
            }
        }
    }
}