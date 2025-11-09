package com.example.piamoviles2.utils

object ValidationUtils {

    /**
     * Valida email con formato estándar
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Valida contraseña según requerimientos del proyecto:
     * - Mínimo 10 caracteres
     * - Al menos una mayúscula
     * - Al menos una minúscula
     * - Al menos un número
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 10) return false

        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasUppercase && hasLowercase && hasDigit
    }

    /**
     * Obtiene mensaje de error específico para contraseña
     */
    fun getPasswordErrorMessage(password: String): String? {
        if (password.length < 10) {
            return "La contraseña debe tener al menos 10 caracteres"
        }
        if (!password.any { it.isUpperCase() }) {
            return "La contraseña debe tener al menos una mayúscula"
        }
        if (!password.any { it.isLowerCase() }) {
            return "La contraseña debe tener al menos una minúscula"
        }
        if (!password.any { it.isDigit() }) {
            return "La contraseña debe tener al menos un número"
        }
        return null // Es válida
    }

    /**
     * Valida que el nombre no esté vacío y tenga al menos 2 caracteres
     */
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    /**
     * Valida que el alias no esté vacío y tenga al menos 3 caracteres
     */
    fun isValidAlias(alias: String): Boolean {
        return alias.trim().length >= 3
    }

    /**
     * Valida teléfono (opcional - solo si se proporciona)
     */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return true // Es opcional
        return phone.matches(Regex("^[0-9]{10}$")) // 10 dígitos
    }
}
