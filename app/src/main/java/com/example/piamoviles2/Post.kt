package com.example.piamoviles2

/**
 * Modelo de datos para las publicaciones/recetas (TEMPORAL PARA MOCK DATA)
 */
data class Post(
    val id: Int,
    val apiId: String? = null, // ID de la publicación en la API (si existe)
    val title: String,
    val description: String,
    val imageUrl: String, // URL de la imagen o recurso drawable
    val author: String,
    val createdAt: String,
    val isOwner: Boolean = false, // Si el post pertenece al usuario actual
    var isFavorite: Boolean = false, // Si está marcado como favorito
    var isDraft: Boolean = false, // Si es un borrador (no publicado)
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
                    apiId = null, //   AGREGAR - Null para datos de ejemplo
                    title = "Tacos al Pastor Deliciosos",
                    description = "Receta tradicional mexicana con carne marinada y piña",
                    imageUrl = "sample_tacos",
                    author = "@ChefMario",
                    createdAt = "2 horas",
                    isOwner = false,
                    isFavorite = true,
                    likesCount = 24,
                    commentsCount = 8
                ),
                Post(
                    id = 2,
                    apiId = null, //   AGREGAR
                    title = "Ensalada César Fresca",
                    description = "Ensalada clásica con aderezo casero y crutones dorados",
                    imageUrl = "sample_salad",
                    author = "@HealthyEats",
                    createdAt = "4 horas",
                    isOwner = true,
                    isFavorite = false,
                    likesCount = 18,
                    commentsCount = 5
                ),
                Post(
                    id = 3,
                    apiId = null, //   AGREGAR
                    title = "Pasta Carbonara Auténtica",
                    description = "Receta italiana original con huevos, panceta y queso",
                    imageUrl = "sample_pasta",
                    author = "@ItalianNonna",
                    createdAt = "1 día",
                    isOwner = false,
                    isFavorite = false,
                    likesCount = 45,
                    commentsCount = 12
                ),
                Post(
                    id = 4,
                    apiId = null, //   AGREGAR
                    title = "Sandwich Club Premium",
                    description = "Sandwich multicapa con pollo, tocino y vegetales frescos",
                    imageUrl = "sample_sandwich",
                    author = "@DeliMaster",
                    createdAt = "2 días",
                    isOwner = false,
                    isFavorite = true,
                    likesCount = 31,
                    commentsCount = 7
                )
            )
        }

        /**
         * Obtener solo las publicaciones favoritas (simulado)
         */
        fun getFavoritePosts(): List<Post> {
            return getSamplePosts().filter { it.isFavorite }
        }

        /**
         * Obtener solo los borradores del usuario actual
         */
        fun getDraftPosts(): List<Post> {
            return getSamplePosts().filter { it.isDraft && it.isOwner }
        }
    }
}