package blockchain.crypto;

import blockchain.util.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Logger;


//https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm
public class ECDSA {

    private static final Logger LOGGER = Logger.getLogger(ECDSA.class.getName());
    private static final int PRIV_KEY_ENCODED_LEN = 288;
    private static final int PUB_KEY_ENCODED_LEN = 176;

    public ECDSA() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public byte[] generateSignature(String plainText, PrivateKey privateKey) throws SignatureException,
            UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaSignature.initSign(privateKey);
        ecdsaSignature.update(plainText.getBytes("UTF-8"));
        byte[] signature = ecdsaSignature.sign();
        return signature;
    }

    public boolean checkSignature(String plainText, PublicKey publicKey, byte[] signature) throws SignatureException,
            InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException{
        Signature ecdsaVerifier = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaVerifier.initVerify(publicKey);
        ecdsaVerifier.update(plainText.getBytes("UTF-8"));
        return ecdsaVerifier.verify(signature);
    }

    // https://en.bitcoin.it/wiki/Secp256k1
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException{
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        generator.initialize(ecSpec, new SecureRandom());
        return generator.generateKeyPair();
    }

    // pkcs8 format required, otherwise method returns false
    // FIXME: definitely need to properly test this method
    public boolean verifyKeys(String privateKey, String publicKey) throws NoSuchProviderException, NoSuchAlgorithmException {

        KeyFactory keyFactory = KeyFactory.getInstance("SHA256withECDSA", "BC");

        try {
            // Decode private key
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
            PrivateKey prvKey = keyFactory.generatePrivate(keySpecPKCS8);

            // Decode public key
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
            PublicKey pubKey = keyFactory.generatePublic(keySpecX509);

            // Generate sample text, sign it and check if the public key passes the signature check
            String sampleText = Utils.generateRandomString(512);
            byte[] sampleSignature = this.generateSignature(sampleText, prvKey);
            boolean result = this.checkSignature(sampleText, pubKey, sampleSignature);
            return result;
        } catch (InvalidKeyException | SignatureException | InvalidKeySpecException | UnsupportedEncodingException e) {
            return false;
        }
    }

    public boolean verifyPublicKeySize(String publicKey) {
        return publicKey.length() == PUB_KEY_ENCODED_LEN;
    }

    public boolean verifyPrivateKeySize(String privateKey) {
        return privateKey.length() == PRIV_KEY_ENCODED_LEN;
    }

}
