package com.ifpr.androidapptemplate.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.androidapptemplate.baseclasses.Item
import com.ifpr.androidapptemplate.databinding.FragmentDashboardBinding
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.location.Location
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private var emojiSelecionado: String = "😐"

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            binding.textDashboard.text = it
        }

        // Deixa os emojis semi-transparentes de início, exceto o padrão (neutro)
        resetarEmojis()
        binding.emojiNeutro.alpha = 1f

        // Cliques dos Emojis
        binding.emojiTriste.setOnClickListener { resetarComSelecao("😢", it) }
        binding.emojiNeutro.setOnClickListener { resetarComSelecao("😐", it) }
        binding.emojiFeliz.setOnClickListener { resetarComSelecao("😊", it) }
        binding.emojiAmor.setOnClickListener { resetarComSelecao("😍", it) }
        binding.emojiRaiva.setOnClickListener { resetarComSelecao("😡", it) }

        // Botões de Ação
        binding.buttonSelectImage.setOnClickListener { openFileChooser() }
        binding.salvarItemButton.setOnClickListener { salvarItem() }
        binding.btnUsarLocalizacao.setOnClickListener { pegarLocalizacaoAtual() }

        return root
    }

    private fun resetarEmojis() {
        binding.emojiTriste.alpha = 0.4f
        binding.emojiNeutro.alpha = 0.4f
        binding.emojiFeliz.alpha = 0.4f
        binding.emojiAmor.alpha = 0.4f
        binding.emojiRaiva.alpha = 0.4f
    }

    private fun resetarComSelecao(emoji: String, view: View) {
        resetarEmojis()
        view.alpha = 1f
        emojiSelecionado = emoji
    }

    private fun openFileChooser() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun salvarItem() {
        val descricao = binding.descricaoEditText.text.toString().trim()
        val categoria = binding.categoriaEditText.text.toString().trim()
        val intensidadeTexto = binding.intensidadeEditText.text.toString()
        val data = binding.dataEditText.text.toString().trim()
        val localizacao = binding.localizacaoEditText.text.toString().trim()

        val intensidade = intensidadeTexto.toIntOrNull() ?: 0

        if (descricao.isEmpty() || categoria.isEmpty() || data.isEmpty() || imageUri == null) {
            Toast.makeText(context, "Por favor, preencha todos os dados e adicione uma foto!", Toast.LENGTH_SHORT).show()
            return
        }

        uploadImage(descricao, categoria, intensidade, data, localizacao)
    }

    private fun uploadImage(descricao: String, categoria: String, intensidade: Int, data: String, localizacao: String) {
        try {
            val inputStream = context?.contentResolver?.openInputStream(imageUri!!)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                val item = Item(
                    descricao = descricao,
                    categoria = categoria,
                    intensidade = intensidade,
                    data = data,
                    foto = base64Image,
                    emoji = emojiSelecionado,
                    localizacao = localizacao
                )
                saveItem(item)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao processar a imagem.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .centerCrop() // Ajusta a imagem dentro do card redondo sem distorcer
                .into(binding.imageItem)
        }
    }

    private fun saveItem(item: Item) {
        databaseReference = FirebaseDatabase.getInstance().getReference("itens")
        val itemId = databaseReference.push().key

        if (itemId != null && auth.currentUser != null) {
            databaseReference.child(auth.uid.toString()).child(itemId).setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(context, "Momento salvo com sucesso! ✨", Toast.LENGTH_SHORT).show()
                    limparFormulario()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao salvar no banco de dados.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun limparFormulario() {
        binding.descricaoEditText.text.clear()
        binding.categoriaEditText.text.clear()
        binding.intensidadeEditText.text.clear()
        binding.dataEditText.text.clear()
        binding.localizacaoEditText.text.clear()
        binding.imageItem.setImageResource(android.R.drawable.ic_menu_gallery)
        imageUri = null
        resetarEmojis()
        binding.emojiNeutro.alpha = 1f
        emojiSelecionado = "😐"
    }

    private fun pegarLocalizacaoAtual() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                converterEndereco(location)
            } else {
                Toast.makeText(context, "Certifique-se de que o GPS está ligado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun converterEndereco(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val endereco = addresses?.firstOrNull()?.getAddressLine(0) ?: "Localização não encontrada"

                withContext(Dispatchers.Main) {
                    binding.localizacaoEditText.setText(endereco)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Falha na rede ao buscar endereço.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}