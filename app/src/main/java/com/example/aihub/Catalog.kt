package com.example.aihub

/** Встроенный каталог известных ИИ-сервисов. */
object Catalog {

    class Def(
        val id: String,
        val title: String,
        val mono: String,
        val bg: Int,
        val fg: Int,
        val url: String,
        val pkgs: List<String>
    )

    val list: List<Def> = listOf(
        Def("claude", "Claude", "Cl", 0xFF3B2A24.toInt(), 0xFFEE8F70.toInt(),
            "https://claude.ai", listOf("com.anthropic.claude")),
        Def("chatgpt", "ChatGPT", "GPT", 0xFF173730.toInt(), 0xFF46D1A8.toInt(),
            "https://chatgpt.com", listOf("com.openai.chatgpt")),
        Def("gemini", "Gemini", "Gm", 0xFF1F2C47.toInt(), 0xFF93B8FA.toInt(),
            "https://gemini.google.com", listOf("com.google.android.apps.bard")),
        Def("perplexity", "Perplexity", "Px", 0xFF15363C.toInt(), 0xFF45D0E0.toInt(),
            "https://www.perplexity.ai", listOf("ai.perplexity.app.android")),
        Def("grok", "Grok", "Gk", 0xFF2C2C31.toInt(), 0xFFE8E8ED.toInt(),
            "https://grok.com", listOf("ai.x.grok")),
        Def("deepseek", "DeepSeek", "DS", 0xFF20284A.toInt(), 0xFF8DA6FF.toInt(),
            "https://chat.deepseek.com", listOf("com.deepseek.chat")),
        Def("copilot", "Copilot", "Co", 0xFF1E3A22.toInt(), 0xFF7FD36A.toInt(),
            "https://copilot.microsoft.com", listOf("com.microsoft.copilot")),
        Def("mistral", "Mistral", "Mi", 0xFF3D2C17.toInt(), 0xFFF7A24A.toInt(),
            "https://chat.mistral.ai", listOf("ai.mistral.chat")),
        Def("meta", "Meta AI", "Ma", 0xFF1B2F4A.toInt(), 0xFF6AAEF7.toInt(),
            "https://www.meta.ai", emptyList())
    )

    fun byId(id: String): Def? = list.firstOrNull { it.id == id }
}
