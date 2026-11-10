package com.palmergames.bukkit.TownyChat.util;

import java.util.Collection;

/** ミュート中の名前の並びを組み立てる。 */
public final class MuteListFormat {
    private MuteListFormat() {}

    /** 名前を区切り文字でつなぐ。空なら空文字を返す。 */
    public static String join(Collection<String> names, String separator) {
        return String.join(separator, names);
    }
}
