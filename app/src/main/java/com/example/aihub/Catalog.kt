package com.example.aihub

import android.content.Context

/** Встроенный каталог известных ИИ-сервисов по категориям. */
object Catalog {

    class Def(
        val id: String,
        val title: String,
        val mono: String,
        val bg: Int,
        val fg: Int,
        val url: String,
        val pkgs: List<String>,
        val playId: String,
        val cat: String
    )

    val cats: List<String> = listOf("chat", "image", "video", "music", "code", "work")

    private val palette = arrayOf(
        intArrayOf(0xFF3A2430.toInt(), 0xFFF28BB0.toInt()),
        intArrayOf(0xFF2F2A14.toInt(), 0xFFE8C547.toInt()),
        intArrayOf(0xFF1E2F3A.toInt(), 0xFF7CC4E8.toInt()),
        intArrayOf(0xFF2A1F3D.toInt(), 0xFFB39AF2.toInt()),
        intArrayOf(0xFF1C3328.toInt(), 0xFF6FD19A.toInt()),
        intArrayOf(0xFF3A2A1C.toInt(), 0xFFF0A868.toInt()),
        intArrayOf(0xFF33201F.toInt(), 0xFFEE8B84.toInt()),
        intArrayOf(0xFF1F3234.toInt(), 0xFF67D1CC.toInt())
    )

    private fun d(
        id: String, title: String, mono: String, url: String, cat: String, p: Int,
        pkgs: List<String> = emptyList(), playId: String = ""
    ): Def {
        val c = palette[p % palette.size]
        return Def(id, title, mono, c[0], c[1], url, pkgs, playId, cat)
    }

    val list: List<Def> = listOf(
        // ---------- Текст и чат ----------
        Def("claude", "Claude", "Cl", 0xFF3B2A24.toInt(), 0xFFEE8F70.toInt(),
            "https://claude.ai", listOf("com.anthropic.claude"), "com.anthropic.claude", "chat"),
        Def("chatgpt", "ChatGPT", "GPT", 0xFF173730.toInt(), 0xFF46D1A8.toInt(),
            "https://chatgpt.com", listOf("com.openai.chatgpt"), "com.openai.chatgpt", "chat"),
        Def("gemini", "Gemini", "Gm", 0xFF1F2C47.toInt(), 0xFF93B8FA.toInt(),
            "https://gemini.google.com", listOf("com.google.android.apps.bard"),
            "com.google.android.apps.bard", "chat"),
        Def("perplexity", "Perplexity", "Px", 0xFF15363C.toInt(), 0xFF45D0E0.toInt(),
            "https://www.perplexity.ai", listOf("ai.perplexity.app.android"), "", "chat"),
        Def("grok", "Grok", "Gk", 0xFF2C2C31.toInt(), 0xFFE8E8ED.toInt(),
            "https://grok.com", listOf("ai.x.grok"), "", "chat"),
        Def("deepseek", "DeepSeek", "DS", 0xFF20284A.toInt(), 0xFF8DA6FF.toInt(),
            "https://chat.deepseek.com", listOf("com.deepseek.chat"), "", "chat"),
        Def("copilot", "Copilot", "Co", 0xFF1E3A22.toInt(), 0xFF7FD36A.toInt(),
            "https://copilot.microsoft.com", listOf("com.microsoft.copilot"), "", "chat"),
        Def("mistral", "Mistral", "Mi", 0xFF3D2C17.toInt(), 0xFFF7A24A.toInt(),
            "https://chat.mistral.ai", listOf("ai.mistral.chat"), "", "chat"),
        Def("meta", "Meta AI", "Ma", 0xFF1B2F4A.toInt(), 0xFF6AAEF7.toInt(),
            "https://www.meta.ai", emptyList(), "", "chat"),
        d("poe", "Poe", "Po", "https://poe.com", "chat", 0),
        d("kimi", "Kimi", "Ki", "https://www.kimi.com", "chat", 1),
        d("qwen", "Qwen", "Qw", "https://chat.qwen.ai", "chat", 3),
        d("characterai", "Character.AI", "Ch", "https://character.ai", "chat", 6),
        d("huggingchat", "HuggingChat", "HF", "https://huggingface.co/chat", "chat", 5),
        d("doubao", "Doubao", "Db", "https://www.doubao.com", "chat", 2),
        d("zai", "Z.ai", "Zi", "https://chat.z.ai", "chat", 4),
        d("youcom", "You.com", "Yo", "https://you.com", "chat", 7),

        // ---------- Картинки ----------
        d("midjourney", "Midjourney", "Mj", "https://www.midjourney.com", "image", 2),
        d("firefly", "Adobe Firefly", "Ff", "https://firefly.adobe.com", "image", 6),
        d("leonardo", "Leonardo", "Le", "https://leonardo.ai", "image", 5),
        d("ideogram", "Ideogram", "Id", "https://ideogram.ai", "image", 4),
        d("krea", "Krea", "Kr", "https://www.krea.ai", "image", 0),
        d("flux", "FLUX", "Fx", "https://bfl.ai", "image", 3),
        d("recraft", "Recraft", "Rc", "https://www.recraft.ai", "image", 1),
        d("stability", "Stability AI", "Sb", "https://stability.ai", "image", 7),
        d("canva", "Canva", "Cv", "https://www.canva.com", "image", 2),
        d("designer", "Microsoft Designer", "Md", "https://designer.microsoft.com", "image", 4),
        d("freepik", "Freepik", "Fp", "https://www.freepik.com/ai", "image", 5),
        d("photoroom", "Photoroom", "Pr", "https://www.photoroom.com", "image", 6),
        d("clipdrop", "Clipdrop", "Cd", "https://clipdrop.co", "image", 0),
        d("remini", "Remini", "Rm", "https://remini.ai", "image", 3),
        d("playground", "Playground", "Pg", "https://playground.com", "image", 1),
        d("mixboard", "Mixboard", "Mb", "https://labs.google.com/mixboard/welcome", "image", 7),

        // ---------- Видео ----------
        d("flow", "Google Flow", "Fl", "https://flow.google", "video", 2),
        d("sora", "Sora", "So", "https://sora.com", "video", 3),
        d("runway", "Runway", "Rw", "https://runwayml.com", "video", 7),
        d("pika", "Pika", "Pk", "https://pika.art", "video", 1),
        d("luma", "Luma", "Lu", "https://lumalabs.ai", "video", 2),
        d("kling", "Kling", "Kl", "https://klingai.com", "video", 4),
        d("hailuo", "Hailuo", "Hl", "https://hailuoai.video", "video", 5),
        d("heygen", "HeyGen", "Hg", "https://www.heygen.com", "video", 6),
        d("vidu", "Vidu", "Vd", "https://www.vidu.com", "video", 0),
        d("hedra", "Hedra", "Hd", "https://www.hedra.com", "video", 3),
        d("higgsfield", "Higgsfield", "Hf", "https://higgsfield.ai", "video", 1),
        d("dreamina", "Dreamina", "Dm", "https://dreamina.capcut.com", "video", 6),
        d("synthesia", "Synthesia", "Sy", "https://www.synthesia.io", "video", 7),
        d("capcut", "CapCut", "Cc", "https://www.capcut.com", "video", 4),
        d("opusclip", "Opus Clip", "Oc", "https://www.opus.pro", "video", 5),
        d("descript", "Descript", "De", "https://www.descript.com", "video", 2),
        d("veed", "VEED", "Ve", "https://www.veed.io", "video", 0),
        d("did", "D-ID", "Di", "https://www.d-id.com", "video", 3),

        // ---------- Музыка и голос ----------
        d("flowmusic", "Flow Music", "FM", "https://flowmusic.app", "music", 2),
        d("suno", "Suno", "Su", "https://suno.com", "music", 0),
        d("udio", "Udio", "Ud", "https://www.udio.com", "music", 3),
        d("elevenlabs", "ElevenLabs", "11", "https://elevenlabs.io", "music", 7),
        d("speechify", "Speechify", "Sp", "https://speechify.com", "music", 2),
        d("stableaudio", "Stable Audio", "Sa", "https://stableaudio.com", "music", 4),
        d("mubert", "Mubert", "Mu", "https://mubert.com", "music", 5),
        d("soundraw", "Soundraw", "Sw", "https://soundraw.io", "music", 6),
        d("aiva", "AIVA", "Av", "https://www.aiva.ai", "music", 1),
        d("murf", "Murf", "Mf", "https://murf.ai", "music", 0),
        d("moises", "Moises", "Mo", "https://moises.ai", "music", 3),
        d("landr", "LANDR", "La", "https://www.landr.com", "music", 7),
        d("wondercraft", "Wondercraft", "Wc", "https://www.wondercraft.ai", "music", 4),

        // ---------- Код ----------
        d("claudecode", "Claude Code", "CC", "https://claude.ai/code", "code", 6),
        d("codex", "Codex", "Cx", "https://chatgpt.com/codex", "code", 4),
        d("githubcopilot", "GitHub Copilot", "GC", "https://github.com/features/copilot", "code", 4),
        d("cursor", "Cursor", "Cu", "https://cursor.com", "code", 7),
        d("windsurf", "Windsurf", "Ws", "https://windsurf.com", "code", 2),
        d("replit", "Replit", "Rp", "https://replit.com", "code", 5),
        d("lovable", "Lovable", "Lv", "https://lovable.dev", "code", 0),
        d("v0", "v0", "v0", "https://v0.dev", "code", 1),
        d("bolt", "Bolt", "Bo", "https://bolt.new", "code", 3),
        d("jules", "Jules", "Ju", "https://jules.google", "code", 2),
        d("aistudio", "Google AI Studio", "AS", "https://aistudio.google.com", "code", 5),
        d("stitch", "Stitch", "St", "https://stitch.withgoogle.com", "code", 6),
        d("opal", "Opal", "Op", "https://opal.withgoogle.com", "code", 7),
        d("devin", "Devin", "Dv", "https://devin.ai", "code", 0),

        // ---------- Исследования и работа ----------
        d("notebooklm", "Gemini Notebook", "GN", "https://notebooklm.google.com", "work", 2),
        d("gamma", "Gamma", "Ga", "https://gamma.app", "work", 3),
        d("consensus", "Consensus", "Cs", "https://consensus.app", "work", 4),
        d("otter", "Otter", "Ot", "https://otter.ai", "work", 7),
        d("deepl", "DeepL", "DL", "https://www.deepl.com", "work", 2),
        d("napkin", "Napkin", "Nk", "https://www.napkin.ai", "work", 5),
        d("fireflies", "Fireflies", "Fi", "https://fireflies.ai", "work", 6),
        d("fathom", "Fathom", "Fa", "https://fathom.video", "work", 1),
        d("grammarly", "Grammarly", "Gr", "https://www.grammarly.com", "work", 4),
        d("notion", "Notion AI", "No", "https://www.notion.so", "work", 0),
        d("beautiful", "Beautiful.ai", "Ba", "https://www.beautiful.ai", "work", 3),
        d("julius", "Julius AI", "Jl", "https://julius.ai", "work", 7),
        d("elicit", "Elicit", "El", "https://elicit.com", "work", 5),
        d("scite", "Scite", "Sc", "https://scite.ai", "work", 1),
        d("quillbot", "QuillBot", "Qb", "https://quillbot.com", "work", 2)
    )

    fun byId(id: String): Def? = list.firstOrNull { it.id == id }

    fun catTitle(context: Context, cat: String): String = context.getString(
        when (cat) {
            "chat" -> R.string.cat_chat
            "image" -> R.string.cat_image
            "video" -> R.string.cat_video
            "music" -> R.string.cat_music
            "code" -> R.string.cat_code
            "work" -> R.string.cat_work
            else -> R.string.cat_custom
        }
    )
}
