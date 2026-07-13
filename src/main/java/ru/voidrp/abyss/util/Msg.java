package ru.voidrp.abyss.util;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

/** Chat helpers: legacy §-code parsing and nick access (26.2 does not interpret
 *  § inside a plain literal). Mirrors voidrp_claims' Claims helpers. */
public final class Msg {

    private Msg() {
    }

    public static String nickLower(Player player) {
        return player.getGameProfile().name().toLowerCase(Locale.ROOT);
    }

    public static String nickRaw(Player player) {
        return player.getGameProfile().name();
    }

    public static void to(Player player, String legacyText) {
        player.sendSystemMessage(legacy(legacyText));
    }

    /** Parses legacy §-color/format codes into a styled Component. */
    public static MutableComponent legacy(String s) {
        MutableComponent out = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                if (buf.length() > 0) {
                    out.append(Component.literal(buf.toString()).setStyle(style));
                    buf.setLength(0);
                }
                ChatFormatting fmt = ChatFormatting.getByCode(Character.toLowerCase(s.charAt(++i)));
                if (fmt == ChatFormatting.RESET) {
                    style = Style.EMPTY;
                } else if (fmt != null) {
                    style = style.applyFormat(fmt);
                }
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            out.append(Component.literal(buf.toString()).setStyle(style));
        }
        return out;
    }
}
