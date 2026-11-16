package com.userfront;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class TransactionMessagesTest {

    @Test
    public void everyTransactionMessageHasAFrenchTranslation() throws Exception {
        Properties english = load("/messages.properties");
        Properties french = load("/messages_fr.properties");
        long checked = 0;
        for (String key : english.stringPropertyNames()) {
            if (!key.startsWith("transaction.")) {
                continue;
            }
            String translation = french.getProperty(key);
            assertNotEquals(null, translation, key);
            assertNotEquals(english.getProperty(key), translation, key);
            checked++;
        }
        assertEquals(french.size(), checked);
    }

    private static Properties load(String path) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = TransactionMessagesTest.class.getResourceAsStream(path)) {
            properties.load(in);
        }
        return properties;
    }
}
