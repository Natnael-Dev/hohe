package app.hohe.keyboard.ime

import app.hohe.keyboard.ime.panels.EmojiCategory

/** Minimal emoji set used only if the bundled Unicode emoji data cannot be read. */
object FallbackEmoji {
    private fun cat(
        id: String,
        icon: String,
        s: String,
    ) = EmojiCategory(id, icon, s.split(' ').filter { it.isNotBlank() })

    val CATEGORIES =
        listOf(
            cat(
                "smileys",
                "😀",
                "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕",
            ),
            cat(
                "people",
                "👍",
                "👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 👀 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵",
            ),
            cat(
                "nature",
                "🐶",
                "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🐴 🦄 🐝 🦋 🐌 🐞 🌸 🌹 🌺 🌻 🌼 🌷 🌱 🌲 🌳 🌴 🌵 🍀 🍁 🍂 🌍 🌙 ⭐ 🌞 ☀️ ⛅ 🌧️ ⛈️ 🌈 🔥 💧 🌊",
            ),
            cat(
                "food",
                "🍎",
                "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🫐 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🥑 🍆 🥔 🥕 🌽 🌶️ 🥒 🥬 🧄 🧅 🍞 🥐 🧀 🥚 🍳 🥞 🥓 🍗 🍖 🍔 🍟 🍕 🌮 🥗 🍝 🍜 🍲 🍛 🍣 🍰 🎂 🍫 🍬 🍩 🍪 ☕ 🍵 🥤 🍺 🍷",
            ),
            cat(
                "activities",
                "⚽",
                "⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥅 ⛳ 🏃 🚴 🏊 🏆 🥇 🥈 🥉 🏅 🎖️ 🎗️ 🎫 🎟️ 🎪 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🎻 🎲 🎯 🎳 🎮 🧩",
            ),
            cat(
                "travel",
                "🚗",
                "🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🛻 🚚 🚛 🚜 🛵 🏍️ 🚲 ✈️ 🛫 🛬 🚀 🛸 🚁 ⛵ 🚤 🚢 🏠 🏡 🏢 🏥 🏦 🏨 🏪 🏫 ⛪ 🕌 🕍 🗽 🗼 🏰 🌉",
            ),
            cat(
                "objects",
                "💡",
                "⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💽 💾 📷 📹 📞 ☎️ 📺 📻 ⏰ ⌛ 💡 🔦 🕯️ 💸 💵 💰 💳 💎 🔧 🔨 🔩 ⚙️ 🔑 🚪 🛏️ 🛋️ 🎁 🎈 🎉 🎊 ✉️ 📦 📝 📚 📖 🔒 🔓",
            ),
            cat(
                "symbols",
                "❤️",
                "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 ✝️ ☪️ ☮️ ☯️ ✡️ 🔯 ♈ ✅ ❌ ❓ ❗ ‼️ ⁉️ 💯 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ ➡️ ⬅️ ⬆️ ⬇️ 🔁 🆗 🆕 🆒",
            ),
            cat(
                "flags",
                "🇪🇹",
                "🇪🇹 🇪🇷 🇰🇪 🇸🇩 🇸🇴 🇩🇯 🇺🇸 🇬🇧 🇨🇦 🇩🇪 🇫🇷 🇮🇹 🇸🇪 🇳🇴 🇮🇱 🇸🇦 🇦🇪 🇨🇳 🇯🇵 🇮🇳 🏳️ 🏴 🏁",
            ),
        )
}
