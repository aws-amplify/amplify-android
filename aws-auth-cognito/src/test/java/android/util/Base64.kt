package android.util

object Base64 {
    @JvmStatic
    fun encode(input: ByteArray?, flags: Int): String {
        return java.util.Base64.getEncoder().encodeToString(input)
    }

    @JvmStatic
    fun decode(str: String?, flags: Int): ByteArray {
        return java.util.Base64.getDecoder().decode(str)
    }
}