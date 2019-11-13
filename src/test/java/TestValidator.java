import blockchain.protocol.Validator;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestValidator {

    @Test
    public void testValidateKeyPair() throws Exception {
        String publicKey = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEd0RUzdEAQMTAe6+HQzGfOQeD8a7SXwu6AN/GIQjiyDqTLgyIoa5PpTuWJTxtYyAuLojyaImfF/AJKZTrvqpsaA==";
        String privateKey = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgOt9GUJ9wsRXYt/Ib5jdiQ79A5Ue65pgtI6DcKsaBXLqgBwYFK4EEAAqhRANCAAR3RFTN0QBAxMB7r4dDMZ85B4PxrtJfC7oA38YhCOLIOpMuDIihrk+lO5YlPG1jIC4uiPJoiZ8X8AkplOu+qmxo";
        Validator validator = new Validator();

        boolean result = validator.validateKeyPair(privateKey, publicKey);

        assertTrue(result);
    }

    @Test
    public void testValidatePublicKeySeparate() {
        String publicKey = "test";
        Validator validator = new Validator();

        boolean result = validator.validatePublicKey(publicKey);

        assertFalse(result);
    }

    @Test
    public void testValidatePrivateKeySeparate() {
        String privateKey = "ab";
        Validator validator = new Validator();

        boolean result = validator.validatePrivateKey(privateKey);

        assertFalse(result);
    }

}
