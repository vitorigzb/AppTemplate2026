package com.ifpr.androidapptemplate.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.util.Base64
import android.widget.*
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Item
import com.ifpr.androidapptemplate.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val containerLayout = binding.itemContainer
        carregarItensMarketplace(containerLayout)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun carregarItensMarketplace(container: LinearLayout) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("itens")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                container.removeAllViews()

                for (userSnapshot in snapshot.children) {
                    for (itemSnapshot in userSnapshot.children) {
                        val item = itemSnapshot.getValue(Item::class.java) ?: continue

                        val itemView = LayoutInflater.from(container.context)
                            .inflate(R.layout.item_template, container, false)

                        val emojiView = itemView.findViewById<TextView>(R.id.item_emoji)
                        val categoriaView = itemView.findViewById<TextView>(R.id.item_categoria)
                        val dataView = itemView.findViewById<TextView>(R.id.item_data)
                        val descricaoView = itemView.findViewById<TextView>(R.id.item_descricao)
                        val intensidadeView = itemView.findViewById<TextView>(R.id.item_intensidade)
                        val imageView = itemView.findViewById<ImageView>(R.id.item_image)

                        val intensidade = item.intensidade ?: 0

                        emojiView.text = item.emoji ?: "😐"

                        categoriaView.text = item.categoria?.uppercase() ?: "SEM CATEGORIA"
                        dataView.text = item.data ?: ""
                        descricaoView.text = item.descricao ?: ""
                        intensidadeView.text = "Intensidade: $intensidade"

                        if (!item.foto.isNullOrEmpty()) {
                            try {
                                val bytes = Base64.decode(item.foto, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                imageView.setImageBitmap(bitmap)
                            } catch (_: Exception) {}
                        }
                        container.addView(itemView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(container.context, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
            }
        })
    }
}