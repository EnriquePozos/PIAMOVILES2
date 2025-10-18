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
                    isFavorite = true,
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
                    isFavorite = false,
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
                    isFavorite = true,
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
                    isFavorite = false,
                    likesCount = 38,
                    commentsCount = 15
                ),
                // Posts del usuario actual (simulado para perfil)
                Post(
                    id = 1001,
                    title = "Tacos al pastor caseros",
                    description = "Mi receta especial de tacos al pastor con ingredientes frescos y marinado casero.",
                    imageUrl = "user_tacos",
                    author = "@Pozos",
                    createdAt = "Hace 1 día",
                    isOwner = true,
                    isFavorite = false,
                    likesCount = 15,
                    commentsCount = 8
                ),
                Post(
                    id = 1002,
                    title = "Desayuno saludable perfecto",
                    description = "Bowl nutritivo perfecto para empezar el día con energía y vitalidad.",
                    imageUrl = "user_breakfast",
                    author = "@Pozos",
                    createdAt = "Hace 3 días",
                    isOwner = true,
                    isFavorite = true,
                    likesCount = 22,
                    commentsCount = 12
                ),
                Post(
                    id = 5,
                    title = "Pizza margherita",
                    description = "Pizza casera con base crujiente, salsa de tomate fresco y mozzarella derretida.",
                    imageUrl = "sample_pizza",
                    author = "Pizzería Casa",
                    createdAt = "Hace 3 días",
                    isFavorite = true,
                    likesCount = 56,
                    commentsCount = 21
                ),
                Post(
                    id = 6,
                    title = "Ensalada César",
                    description = "Ensalada fresca con pollo grillado, crutones caseros y aderezo César tradicional.",
                    imageUrl = "sample_cesar",
                    author = "Green Life",
                    createdAt = "Hace 5 días",
                    isFavorite = false,
                    likesCount = 31,
                    commentsCount = 9
                )
            )
        }

        /**
         * Obtener solo las publicaciones favoritas (simulado)
         */
        fun getFavoritePosts(): List<Post> {
            return getSamplePosts().filter { it.isFavorite }
        }
    }
}
