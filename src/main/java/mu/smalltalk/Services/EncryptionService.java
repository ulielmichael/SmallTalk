package mu.smalltalk.Services;



import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mu.smalltalk.Aes256;






public class EncryptionService {
    private final Aes256 encryptionEngine;
    private final ExecutorService executorService;

    /**
     * Creates a new encryption service with the provided AES-256 implementation
     * 
     * @param encryptionEngine The AES-256 implementation to use for encryption/decryption
     */
    public EncryptionService(Aes256 encryptionEngine) {
        this.encryptionEngine = encryptionEngine;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * Encrypts the provided data asynchronously
     * 
     * @param dataToEncrypt The string data to encrypt
     * @return A CompletableFuture that will contain the encrypted data
     */
    public CompletableFuture<byte[]> encryptAsync(byte[] dataToEncrypt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Encrypting data of length: " + dataToEncrypt.length);
                byte[] encrypted = encryptionEngine.encrypt(dataToEncrypt);
                System.out.println("Encryption complete. Result length: " + encrypted.length);
                return encrypted;
            } catch (Exception e) {
                System.err.println("Encryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to encrypt data", e);
            }
        }, executorService);
    }

    /**
     * Decrypts the provided data asynchronously
     * 
     * @param encryptedData The encrypted string to decrypt
     * @return A CompletableFuture that will contain the decrypted data
     */
    public CompletableFuture<byte[]> decryptAsync(byte[] encryptedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Decrypting data of length: " + encryptedData.length);
                byte[] decrypted = encryptionEngine.decrypt(encryptedData);
                System.out.println("Decryption complete. Result length: " + decrypted.length);
                return decrypted;
            } catch (Exception e) {
                System.err.println("Decryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to decrypt data", e);
            }
        }, executorService);
    }

    
    public void shutdown() {
        executorService.shutdown();
    }
}