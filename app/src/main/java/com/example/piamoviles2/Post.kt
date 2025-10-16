package com.example.piamoviles2

/**
 * Modelo de datos para las publicaciones/recetas (TEMPORAL PARA MOCK DATA)
 */
data class Post(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String, // URL de la imagen o recurso drawable
    val author: String,
    val createdAt: String,
    val isOwner: Boolean = false, // Si el post pertenece al usuario actual
    var isFavorite: Boolean = false, // Si está marcado como favorito
    val likesCount: Int = 0,
    val commentsCount: Int = 0
) {
    companion object {
        /**
         * Datos de ejemplo para pruebas
         */
        fun getSamplePosts(): List<Post> {
            return listOf(
                Post(
                    id = 1,
                    title = "Tacos al pastor",
                    description = "Deliciosos tacos al pastor con piña, cebolla y cilantro. Perfectos para compartir en familia.",
                    imageUrl = "sample_tacos", // Referencia a drawable
                    author = "Chef María",
                    createdAt = "Hace 2 horas",
                    likesCount = 24,
                    commentsCount = 8
                ),
                Post(
                    id = 2,
                    title = "Sandwich",
                    description = "Sandwich gourmet con ingredientes frescos y pan artesanal tostado.",
                    imageUrl = "sample_sandwich",
                    author = "Cocina Express",
                    createdAt = "Hace 4 horas",
                    likesCount = 15,
                    commentsCount = 3
                ),
                Post(
                    id = 3,
                    title = "Desayuno saludable",
                    description = "Bowl nutritivo con frutas frescas, granola casera y yogurt natural. Ideal para empezar el día.",
                    imageUrl = "sample_salad",
                    author = "Vida Sana",
                    createdAt = "Hace 1 día",
                    likesCount = 42,
                    commentsCount = 12
                ),
                Post(
                    id = 4,
                    title = "Pasta italiana",
                    description = "Pasta casera con salsa de tomate y albahaca fresca. Receta tradicional italiana.",
                    imageUrl = "sample_pasta",
                    author = "Nonna's Kitchen",
                    createdAt = "Hace 2 días",
                    likesCount = 38,
                    commentsCount = 15
                )
            )
        }
    }
}
