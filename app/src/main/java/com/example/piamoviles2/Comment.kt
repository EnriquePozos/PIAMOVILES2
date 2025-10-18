package com.example.piamoviles2

/**
 * Modelo de datos para comentarios con respuestas anidadas
 */
data class Comment(
    val id: Int,
    val user: String,
    val text: String,
    val timestamp: String,
    var userLikeState: LikeState = LikeState.NONE,
    val replies: MutableList<Reply> = mutableListOf()
) {

    enum class LikeState {
        NONE, LIKED, DISLIKED
    }

    /**
     * Modelo para respuestas a comentarios
     */
    data class Reply(
        val id: Int,
        val user: String,
        val text: String,
        val timestamp: String,
        var userLikeState: LikeState = LikeState.NONE
    )

    companion object {
        /**
         * Datos de ejemplo para comentarios
         */
        fun getSampleComments(): List<Comment> {
            return listOf(
                Comment(
                    id = 1,
                    user = "Enrique Pozos",
                    text = "lorem ipsum, quia dolor sit, amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisl ut aliquid ex ea",
                    timestamp = "Hace 2 horas",
                    userLikeState = LikeState.NONE,
                    replies = mutableListOf(
                        Reply(
                            id = 101,
                            user = "Raul García 32",
                            text = "lorem ipsum, quia dolor sit, amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisl ut aliquid ex ea",
                            timestamp = "Hace 1 hora",
                            userLikeState = LikeState.NONE
                        )
                    )
                ),
                Comment(
                    id = 2,
                    user = "María González",
                    text = "¡Se ve delicioso! Definitivamente voy a probar esta receta este fin de semana.",
                    timestamp = "Hace 30 minutos",
                    userLikeState = LikeState.NONE,
                    replies = mutableListOf()
                ),
                Comment(
                    id = 3,
                    user = "Carlos Ruiz",
                    text = "Excelente presentación, me encanta cómo se ve todo tan fresco y colorido.",
                    timestamp = "Hace 15 minutos",
                    userLikeState = LikeState.NONE,
                    replies = mutableListOf(
                        Reply(
                            id = 301,
                            user = "Ana López",
                            text = "Totalmente de acuerdo, la presentación es clave.",
                            timestamp = "Hace 10 minutos",
                            userLikeState = LikeState.NONE
                        ),
                        Reply(
                            id = 302,
                            user = "Pedro Martín",
                            text = "Sí, y además se ve muy saludable.",
                            timestamp = "Hace 5 minutos",
                            userLikeState = LikeState.NONE
                        )
                    )
                )
            )
        }
    }
}