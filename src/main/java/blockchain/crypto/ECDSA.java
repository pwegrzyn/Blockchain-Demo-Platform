package blockchain.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.io.UnsupportedEncodingException;
import java.security.*;

//https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm
public class ECDSA {

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

}
