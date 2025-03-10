package mu.smalltalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class Aes256 {
    private static final int Nb = 4;
    private static final int Nk = 8;
    private static final int Nr = 14;

    public Aes256(byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("Invalid key length (should be 32 bytes)");
        }
        expandedKey = expandKey(key);
    }

    private static final int[] SBOX = {
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    };
    private static final int[] INV_SBOX = {
            0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
            0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
            0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
            0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
            0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
            0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
            0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
            0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
            0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
            0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
            0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
            0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
            0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
            0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
            0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
            0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
    };

    private void subBytes(int[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] = SBOX[state[row][col]];
            }
        }
    }

    private void shiftRows(int[][] state) {
        int temp;

        temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;

        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;

        temp = state[3][0];
        state[3][0] = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = temp;
    }

    private void mixColumns(int[][] state) {
        for (int c = 0; c < 4; c++) {
            int a0 = state[0][c];
            int a1 = state[1][c];
            int a2 = state[2][c];
            int a3 = state[3][c];

            state[0][c] = mul(0x02, a0) ^ mul(0x03, a1) ^ a2 ^ a3;
            state[1][c] = a0 ^ mul(0x02, a1) ^ mul(0x03, a2) ^ a3;
            state[2][c] = a0 ^ a1 ^ mul(0x02, a2) ^ mul(0x03, a3);
            state[3][c] = mul(0x03, a0) ^ a1 ^ a2 ^ mul(0x02, a3);
        }
    }

    private void addRoundKey(int[][] state, int[] roundKey, int round) {
        for (int c = 0; c < 4; c++) {
            int word = roundKey[round * 4 + c];
            state[0][c] ^= (word >> 24) & 0xff;
            state[1][c] ^= (word >> 16) & 0xff;
            state[2][c] ^= (word >> 8) & 0xff;
            state[3][c] ^= word & 0xff;
        }
    }

    private static final int[] RCON = {
            0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36,
            0x6c, 0xd8, 0xab, 0x4d, 0x9a, 0x2f, 0x5e, 0xbc, 0x63, 0xc6
    };

    private int[] expandedKey;

    private int[] expandKey(byte[] key) {
        int[] w = new int[Nb * (Nr + 1)];
        int temp;

        int i = 0;
        while (i < Nk) {
            w[i] = ((key[4 * i] & 0xff) << 24) |
                    ((key[4 * i + 1] & 0xff) << 16) |
                    ((key[4 * i + 2] & 0xff) << 8) |
                    (key[4 * i + 3] & 0xff);
            i++;
        }

        i = Nk;
        while (i < Nb * (Nr + 1)) {
            temp = w[i - 1];
            if (i % Nk == 0) {
                temp = subWord(rotWord(temp)) ^ RCON[i / Nk - 1];
            } else if (Nk > 6 && i % Nk == 4) {
                temp = subWord(temp);
            }
            w[i] = w[i - Nk] ^ temp;
            i++;
        }

        return w;
    }

    private int subWord(int word) {
        return (SBOX[(word >> 24) & 0xff] << 24) |
                (SBOX[(word >> 16) & 0xff] << 16) |
                (SBOX[(word >> 8) & 0xff] << 8) |
                SBOX[word & 0xff];
    }

    private int rotWord(int word) {
        return (word << 8) | ((word >> 24) & 0xff);
    }

    public byte[] encrypt(byte[] input) {
        if (input.length != 16) {
            throw new IllegalArgumentException("Input length must be 16 bytes");
        }

        byte[] output = new byte[16];
        int[][] state = new int[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[j][i] = input[i * 4 + j] & 0xff;
            }
        }

        addRoundKey(state, expandedKey, 0);

        for (int round = 1; round < Nr; round++) {
            subBytes(state);
            shiftRows(state);
            mixColumns(state);
            addRoundKey(state, expandedKey, round);
        }

        subBytes(state);
        shiftRows(state);
        addRoundKey(state, expandedKey, Nr);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                output[i * 4 + j] = (byte) state[j][i];
            }
        }

        return output;
    }

    private byte[] padInput(byte[] input) {
        int padLength = 16 - (input.length % 16);
        byte[] paddedInput = Arrays.copyOf(input, input.length + padLength);

        for (int i = input.length; i < paddedInput.length; i++) {
            paddedInput[i] = (byte) padLength;
        }

        return paddedInput;
    }

    private byte[] removePadding(byte[] input) {
        if (input.length == 0 || input.length % 16 != 0) {
            throw new IllegalArgumentException("Invalid input length");
        }

        int padLength = input[input.length - 1] & 0xff;

        if (padLength < 1 || padLength > 16) {
            throw new IllegalArgumentException("Invalid padding length");
        }

        for (int i = input.length - padLength; i < input.length; i++) {
            if ((input[i] & 0xff) != padLength) {
                throw new IllegalArgumentException("Invalid padding values");
            }
        }

        return Arrays.copyOf(input, input.length - padLength);
    }

    public byte[] decrypt(byte[] input) {
        if (input.length != 16) {
            throw new IllegalArgumentException("Input length must be 16 bytes");
        }

        byte[] output = new byte[16];
        int[][] state = new int[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[j][i] = input[i * 4 + j] & 0xff;
            }
        }

        addRoundKey(state, expandedKey, Nr);

        for (int round = Nr - 1; round > 0; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, expandedKey, round);
            invMixColumns(state);
        }

        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, expandedKey, 0);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                output[i * 4 + j] = (byte) state[j][i];
            }
        }

        return output;
    }

    public byte[] encryptString(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] paddedInput = padInput(textBytes);
        byte[] result = new byte[paddedInput.length];

        for (int i = 0; i < paddedInput.length; i += 16) {
            byte[] block = Arrays.copyOfRange(paddedInput, i, i + 16);
            byte[] encryptedBlock = encrypt(block);
            System.arraycopy(encryptedBlock, 0, result, i, 16);
        }

        return result;
    }

    public String decryptToString(byte[] encrypted) {
        if (encrypted.length % 16 != 0) {
            throw new IllegalArgumentException("Encrypted data length must be multiple of 16");
        }

        byte[] decrypted = new byte[encrypted.length];

        for (int i = 0; i < encrypted.length; i += 16) {
            byte[] block = Arrays.copyOfRange(encrypted, i, i + 16);
            byte[] decryptedBlock = decrypt(block);
            System.arraycopy(decryptedBlock, 0, decrypted, i, 16);
        }

        return new String(removePadding(decrypted), StandardCharsets.UTF_8);
    }

    private void invMixColumns(int[][] state) {
        for (int c = 0; c < 4; c++) {
            int a0 = state[0][c];
            int a1 = state[1][c];
            int a2 = state[2][c];
            int a3 = state[3][c];

            state[0][c] = mul(0x0e, a0) ^ mul(0x0b, a1) ^ mul(0x0d, a2) ^ mul(0x09, a3);
            state[1][c] = mul(0x09, a0) ^ mul(0x0e, a1) ^ mul(0x0b, a2) ^ mul(0x0d, a3);
            state[2][c] = mul(0x0d, a0) ^ mul(0x09, a1) ^ mul(0x0e, a2) ^ mul(0x0b, a3);
            state[3][c] = mul(0x0b, a0) ^ mul(0x0d, a1) ^ mul(0x09, a2) ^ mul(0x0e, a3);
        }
    }

    private void invSubBytes(int[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                state[row][col] = INV_SBOX[state[row][col]];
            }
        }
    }

    private void invShiftRows(int[][] state) {
        int temp;

        temp = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = temp;

        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;

        temp = state[3][3];
        state[3][3] = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = temp;
    }

    private int mul(int a, int b) {
        int result = 0;
        int temp = b;
        while (a != 0) {
            if ((a & 1) != 0) {
                result ^= temp;
            }
            temp = xtime(temp);
            a >>= 1;
        }
        return result;
    }

    private int xtime(int b) {
        return ((b << 1) ^ (((b & 0x80) != 0) ? 0x1b : 0)) & 0xff;
    }

    public void encryptFile(File inputFile, File outputFile) throws IOException {
        byte[] fileContent = Files.readAllBytes(inputFile.toPath());

        int paddedLength = ((fileContent.length + 15) / 16) * 16; // מעגל למעלה ל-16
        byte[] paddedContent = new byte[paddedLength];
        System.arraycopy(fileContent, 0, paddedContent, 0, fileContent.length);

        byte[] encryptedContent = new byte[paddedLength];
        for (int i = 0; i < paddedLength; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(paddedContent, i, block, 0, 16);
            byte[] encryptedBlock = encrypt(block);
            System.arraycopy(encryptedBlock, 0, encryptedContent, i, 16);
        }

        Files.write(outputFile.toPath(), encryptedContent);
    }

    public void decryptFile(File inputFile, File outputFile) throws IOException {
        byte[] encryptedContent = Files.readAllBytes(inputFile.toPath());

        if (encryptedContent.length % 16 != 0) {
            throw new IllegalArgumentException("The encrypted file must have a length that is divisible by 16");
        }

        byte[] decryptedContent = new byte[encryptedContent.length];
        for (int i = 0; i < encryptedContent.length; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(encryptedContent, i, block, 0, 16);
            byte[] decryptedBlock = decrypt(block);
            System.arraycopy(decryptedBlock, 0, decryptedContent, i, 16);
        }

        Files.write(outputFile.toPath(), decryptedContent);
    }

    public static void main(String[] args) {
        try {
            byte[] key = new byte[32];
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) i;
            }

            Aes256 aes = new Aes256(key);
            String originalText = """
                    """;

            System.out.println("Original text: " + originalText);

            byte[] encrypted = aes.encryptString(originalText);
            System.out.println("Encrypted ( to hex):");
            for (byte b : encrypted) {
                System.out.printf("%02X ", b);
            }
            System.out.println();

            String decrypted = aes.decryptToString(encrypted);
            System.out.println("Decrypted: " + decrypted);

            if (originalText.equals(decrypted)) {
                System.out.println("Success Encryption/decryption worked correct");
            } else {
                System.out.println("Error: Decrypted text doesn't match original");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}