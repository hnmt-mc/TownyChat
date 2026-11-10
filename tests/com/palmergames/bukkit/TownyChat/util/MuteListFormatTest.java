package com.palmergames.bukkit.TownyChat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MuteListFormatTest {
    @Test
    void 複数の名前は区切りでつなぐ() {
        assertEquals("a|b|c", MuteListFormat.join(List.of("a", "b", "c"), "|"));
    }

    @Test
    void 一人だけなら区切りを付けない() {
        assertEquals("a", MuteListFormat.join(List.of("a"), "|"));
    }

    @Test
    void 空なら空文字() {
        assertEquals("", MuteListFormat.join(List.of(), "|"));
    }
}
