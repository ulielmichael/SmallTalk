package mu.smalltalk.Services;



import java.security.*;
import java.security.spec.*;
import javax.crypto.KeyAgreement;

public class ECDHService {
    private static final String KEY_AGREEMENT_ALGORITHM = "ECDH";
    private static final String EC_CURVE_NAME = "secp256r1"; // NIST P-256
    private static final String KEY_GENERATION_ALGORITHM = "EC";
    
    private KeyPair keyPair;
    private KeyAgreement keyAgreement;

    public ECDHService() throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        initialize();
    }

    private void initialize() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        // Initialize EC domain parameters
        ECGenParameterSpec ecParameterSpec = new ECGenParameterSpec(EC_CURVE_NAME);
        
        // Generate key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyPairGenerator.initialize(ecParameterSpec);
        this.keyPair = keyPairGenerator.generateKeyPair();
        
        // Initialize key agreement
        keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM);
        keyAgreement.init(keyPair.getPrivate());
    }

    public byte[] getPublicKeyEncoded() {
        return keyPair.getPublic().getEncoded();
    }

    /**
     * Derive a shared secret key using our private key and other party's public key
     * @param otherPublicKeyBytes Encoded public key from other party
     * @return Derived shared secret bytes
     */
    public byte[] deriveSharedSecret(byte[] otherPublicKeyBytes) 
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        
        // Convert bytes to PublicKey
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(otherPublicKeyBytes);
        PublicKey otherPublicKey = keyFactory.generatePublic(publicKeySpec);
        
        // Generate shared secret
        keyAgreement.doPhase(otherPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();
        
        // We now have a shared secret, but it's usually used to derive an AES key
        return hashSharedSecret(sharedSecret);
    }
    
    /**
     * Hash the shared secret to derive an AES-256 key (32 bytes)
     */
    private byte[] hashSharedSecret(byte[] sharedSecret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(sharedSecret);
    }
    
    /**
     * Generate new ECDH key pair
     */
    public void generateNewKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        initialize();  // Generate new key pair
    }
    
    /**
     * Get the private key as encoded bytes (only for backup purposes)
     * WARNING: The private key should be handled very carefully
     */
    public byte[] getPrivateKeyEncoded() {
        return keyPair.getPrivate().getEncoded();
    }
    
    /**
     * Import a private key from encoded bytes (for restoring from backup)
     */
    public void importPrivateKey(byte[] privateKeyBytes) 
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        
        // We need to recreate the public key as well - using the private key material
        // Note: This is a simplified approach - in a real implementation you would
        // extract the public key points from the private key and reconstruct it
        
        // For now, we'll just re-initialize with the private key
        keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM);
        keyAgreement.init(privateKey);
        
        // We're not fully recreating the key pair, just storing the private key
        // The public key will need to be extracted separately
    }
}