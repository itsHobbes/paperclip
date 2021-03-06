package uk.co.markg.paperclip.listener;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ListernersTest {

    @Test
    public void testValidListeners() {
        Object[] listeners = Listeners.getListeners("uk.co.markg.paperclip.listener.valid");
        assertTrue(listeners.length == 1);
    }

    @Test
    public void testInvalidPackage() {
        Object[] listeners =
                Listeners.getListeners("uk.co.markg.paperclip.listener.invalidpackage");
        assertTrue(listeners.length == 0);
    }

}
