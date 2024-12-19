package com.example.seguimientoderutas

data class Ruta(
    val id: String? = null,
    val nombreRuta: String? = null,
    val fecha: String? = null,
    val coordenadas: List<Coordenada>? = null  // Asegúrate de que Coordenada esté definida
)
