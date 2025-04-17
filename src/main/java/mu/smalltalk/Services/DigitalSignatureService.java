package mu.smalltalk.Services;


import java.security.*;
import java.security.spec.*;

public class DigitalSignatureService {
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final String KEY_ALGORITHM = "EC";
    private static final String EC_CURVE_NAME = "secp256r1"; // NIST P-256
    
    private KeyPair keyPair;
    
    public DigitalSignatureService() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        generateKeys();
    }
    
    public void generateKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec(EC_CURVE_NAME);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(ecParameterSpec);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }
    
    /**
     * Sign data with the private key
     * @param data The data to sign
     * @return The signature bytes
     */
    public byte[] signData(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(data);
        return signature.sign();
    }
    
    /**
     * Verify a signature using a public key
     * @param data The original data
     * @param signatureBytes The signature to verify
     * @param publicKeyBytes The encoded public key to use for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(byte[] data, byte[] signatureBytes, byte[] publicKeyBytes) 
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        
        // Convert bytes to PublicKey
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        
        // Verify signature
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
    
    /**
     * Get the public key bytes
     * @return Encoded public key
     */
    public byte[] getPublicKeyEncoded() {
        return keyPair.getPublic().getEncoded();
    }
    
    /**
     * Get the private key bytes (for backup purposes only)
     * @return Encoded private key
     */
    public byte[] getPrivateKeyEncoded() {
        return keyPair.getPrivate().getEncoded();
    }
    
    /**
     * Import a private key from encoded bytes
     * @param privateKeyBytes Encoded private key
     */
    public void importPrivateKey(byte[] privateKeyBytes) 
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        
        // Note: In a real implementation, we would also reconstruct the public key
        // from the private key. For simplicity, we're not doing that here.
    }
}