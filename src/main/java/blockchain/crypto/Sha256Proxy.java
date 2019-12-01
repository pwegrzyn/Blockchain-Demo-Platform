package blockchain.crypto;

import blockchain.crypto.sha.Sha256;
import blockchain.crypto.sha.Sha256NonceSearching;
import blockchain.crypto.sha.Sha256WithNonce;

public class Sha256Proxy {
    private static final Sha256 sha256 = new Sha256();
    private static final Sha256WithNonce sha256WithNonce = new Sha256WithNonce();
    private static final Sha256NonceSearching sha256NonceSearching = new Sha256NonceSearching(1000);

    public static String calculateShaHash(String input) {
        synchronized (sha256) {
            sha256.setData(input);
            return sha256.crypt();
        }
    }

    public static String calculateShaHashWithNonce(String input, int nonce) {
        synchronized (sha256WithNonce) {
            sha256WithNonce.setData(input, nonce);
            return sha256WithNonce.crypt();
        }
    }

    public static int searchForNonce(String input, String target) {
        synchronized (sha256NonceSearching) {
            sha256NonceSearching.setData(input, target);
            return sha256NonceSearching.crypt();
        }
    }
}
