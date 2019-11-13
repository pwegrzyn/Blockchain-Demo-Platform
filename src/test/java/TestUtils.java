import blockchain.util.Utils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestUtils {

    @Test
    public void testGenerateRandomString() {
        int length = 15;

        String generatedString = Utils.generateRandomString(length);

        assertEquals(length, generatedString.length());
    }

    @Test
    public void testGenerateRandomStringEmpty() {
        assertEquals(Utils.generateRandomString(0), "");
    }

    @Test
    public void testValidIP() {
        String validIP = "1.1.1.1";

        boolean result = Utils.validIP(validIP);

        assertTrue(result);
    }

    @Test
    public void testInvalidIP() {
        String invalidIP = "1.260.0.0";

        boolean result = Utils.validIP(invalidIP);

        assertFalse(result);
    }

    @Test
    public void testInvalidIPtooLong() {
        String invalidIP = "1.1.1.1.1";

        boolean result = Utils.validIP(invalidIP);

        assertFalse(result);
    }

    @Test
    public void testInvalidIPChars() {
        String invalidIP = "a.1.1.1";

        boolean result = Utils.validIP(invalidIP);

        assertFalse(result);
    }

    @Test
    public void testHexStringToByteArray() {
        String hexString = "0808";
        byte[] expectedByteArray = {8, 8};

        byte[] byteArray = Utils.hexStringToByteArray(hexString);

        assertArrayEquals(byteArray, expectedByteArray);
    }

    @Test
    public void testHexStringToByteArrayEmpty() {
        String hexString = "";
        byte[] expectedByteArray = {};

        byte[] byteArray = Utils.hexStringToByteArray(hexString);

        assertArrayEquals(byteArray, expectedByteArray);
    }

}
