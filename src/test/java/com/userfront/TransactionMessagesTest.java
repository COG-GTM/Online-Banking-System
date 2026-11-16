package com.userfront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

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
            assertNotEquals(key, null, translation);
            assertNotEquals(key, english.getProperty(key), translation);
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
