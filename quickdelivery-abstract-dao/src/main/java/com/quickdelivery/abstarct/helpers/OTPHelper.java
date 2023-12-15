package com.quickdelivery.abstarct.helpers;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OTPHelper {
    public static String generateOTP(String secret, long counter) throws NoSuchAlgorithmException {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(counter);
        byte[] counterBytes = buffer.array();
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(secret.getBytes());
        byte[] hash = md.digest(counterBytes);
        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16) | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
        return String.format("%06d", binary % 1000000);
    }
}
