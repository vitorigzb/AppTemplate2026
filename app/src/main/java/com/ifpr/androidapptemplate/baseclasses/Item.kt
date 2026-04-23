package com.ifpr.androidapptemplate.baseclasses

data class Item(
    var descricao: String? = null,
    var categoria: String? = null,
    var intensidade: Int? = null,
    var data: String? = null,
    var foto: String? = null,
    var emoji: String? = null,
    var localizacao: String? = null
)
