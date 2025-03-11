package mu.smalltalk.Services;

import java.util.Arrays;
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
     * Pads the input data to be a multiple of 16 bytes (AES block size)
     * 
     * @param input The input data to pad
     * @return The padded data
     */
    private byte[] padData(byte[] input) {
        int padLength = 16 - (input.length % 16);
        byte[] paddedInput = Arrays.copyOf(input, input.length + padLength);
        
        for (int i = input.length; i < paddedInput.length; i++) {
            paddedInput[i] = (byte) padLength;
        }
        
        return paddedInput;
    }
    
    /**
     * Encrypts the provided data asynchronously
     * 
     * @param dataToEncrypt The byte array to encrypt
     * @return A CompletableFuture that will contain the encrypted data
     */
    public CompletableFuture<byte[]> encryptAsync(byte[] dataToEncrypt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Encrypting data of length: " + dataToEncrypt.length);
                
                // Pad the data to be a multiple of 16 bytes
                byte[] paddedData = padData(dataToEncrypt);
                byte[] result = new byte[paddedData.length];
                
                // Encrypt each 16-byte block
                for (int i = 0; i < paddedData.length; i += 16) {
                    byte[] block = Arrays.copyOfRange(paddedData, i, i + 16);
                    byte[] encryptedBlock = encryptionEngine.encrypt(block);
                    System.arraycopy(encryptedBlock, 0, result, i, 16);
                }
                
                System.out.println("Encryption complete. Result length: " + result.length);
                return result;
            } catch (Exception e) {
                System.err.println("Encryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to encrypt data", e);
            }
        }, executorService);
    }

    /**
     * Removes padding from decrypted data according to PKCS#7 padding scheme
     * 
     * @param paddedData The padded data
     * @return The data with padding removed
     */
    private byte[] removePadding(byte[] paddedData) {
        if (paddedData.length == 0 || paddedData.length % 16 != 0) {
            throw new IllegalArgumentException("Invalid padded data length");
        }

        int padLength = paddedData[paddedData.length - 1] & 0xFF;
        
        if (padLength < 1 || padLength > 16) {
            throw new IllegalArgumentException("Invalid padding length: " + padLength);
        }
        
        // Verify the padding
        for (int i = paddedData.length - padLength; i < paddedData.length; i++) {
            if ((paddedData[i] & 0xFF) != padLength) {
                throw new IllegalArgumentException("Invalid padding values");
            }
        }
        
        return Arrays.copyOf(paddedData, paddedData.length - padLength);
    }

    /**
     * Decrypts the provided data asynchronously
     * 
     * @param encryptedData The encrypted data to decrypt
     * @return A CompletableFuture that will contain the decrypted data
     */
    public CompletableFuture<byte[]> decryptAsync(byte[] encryptedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Decrypting data of length: " + encryptedData.length);
                
                if (encryptedData.length % 16 != 0) {
                    throw new IllegalArgumentException("Encrypted data length must be multiple of 16");
                }
                
                byte[] decrypted = new byte[encryptedData.length];
                
                // Decrypt each 16-byte block
                for (int i = 0; i < encryptedData.length; i += 16) {
                    byte[] block = Arrays.copyOfRange(encryptedData, i, i + 16);
                    byte[] decryptedBlock = encryptionEngine.decrypt(block);
                    System.arraycopy(decryptedBlock, 0, decrypted, i, 16);
                }
                
                // Remove padding
                byte[] unpaddedData = removePadding(decrypted);
                
                System.out.println("Decryption complete. Result length: " + unpaddedData.length);
                return unpaddedData;
            } catch (Exception e) {
                System.err.println("Decryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to decrypt data", e);
            }
        }, executorService);
    }
    
    /**
     * Encrypts a string asynchronously 
     * 
     * @param text The string to encrypt
     * @return A CompletableFuture that will contain the encrypted data
     */
    public CompletableFuture<byte[]> encryptStringAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Encrypting string of length: " + text.length());
                byte[] encrypted = encryptionEngine.encryptString(text);
                System.out.println("Encryption complete. Result length: " + encrypted.length);
                return encrypted;
            } catch (Exception e) {
                System.err.println("String encryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to encrypt string", e);
            }
        }, executorService);
    }
    
    /**
     * Decrypts data to a string asynchronously
     * 
     * @param encryptedData The encrypted data
     * @return A CompletableFuture that will contain the decrypted string
     */
    public CompletableFuture<String> decryptToStringAsync(byte[] encryptedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Decrypting to string, data length: " + encryptedData.length);
                String decrypted = encryptionEngine.decryptToString(encryptedData);
                System.out.println("Decryption complete. Result length: " + decrypted.length());
                return decrypted;
            } catch (Exception e) {
                System.err.println("String decryption failed: " + e.getMessage());
                throw new RuntimeException("Failed to decrypt to string", e);
            }
        }, executorService);
    }

    /**
     * Shuts down the executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}