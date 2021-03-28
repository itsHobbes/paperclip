package uk.co.markg.paperclip;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class ListernersTest {
    
    @Test
    public void testValidListeners() {
        Object[] listeners = Listeners.getListeners("uk.co.markg.paperclip.valid");
        assertTrue(listeners.length == 1);
    }
    
    @Test
    public void testInvalidPackage() {
        Object[] listeners = Listeners.getListeners("uk.co.markg.paperclip.invalidpackage");
        assertTrue(listeners.length == 0);
    }
    
}
