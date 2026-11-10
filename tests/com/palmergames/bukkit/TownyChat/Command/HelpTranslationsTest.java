package com.palmergames.bukkit.TownyChat.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HelpTranslationsTest {
    @Test
    void 二言語のキーが同じ() {
        var all = HelpTranslations.texts();
        assertEquals(all.get("en_US").keySet(), all.get("ja_JP").keySet());
    }

    @Test
    void 説明文が空でない() {
        HelpTranslations.texts().values().forEach(map ->
                map.values().forEach(v -> assertFalse(v.isBlank())));
    }

    @Test
    void ヘルプで使うキーがすべて登録されている() {
        var keys = HelpTranslations.texts().get("en_US").keySet();
        for (String[] entry : ChannelCommand.helpEntries()) {
            assertTrue(keys.contains(entry[1]), entry[1]);
        }
    }
}
