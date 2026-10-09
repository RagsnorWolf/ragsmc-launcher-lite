package com.ragsmc.launcher.auth;

public class OfflineUuid {
    
    public static String generate(String username) {
        // UUID v3 para modo offline
        // Formato: Username-based UUID v3
        long mostSigBits = username.hashCode();
        long leastSigBits = 0;
        
        // Establecer version 3 (MD5 based)
        leastSigBits &= 0xffffffffL;
        mostSigBits &= 0x000fffffffffffffL;
        mostSigBits |= 0x3000000000000000L; // version 3
        
        // Establecer variante
        mostSigBits &= 0xffffffffffffcL;
        mostSigBits |= 0x8000000000000000L;
        
        String uuid = String.format("%08x-%04x-%04x-%04x-%012x",
            (mostSigBits & 0xffffffffL) >> 32,
            (mostSigBits & 0xffff0000L) >> 16,
            mostSigBits & 0xffffL,
            leastSigBits & 0xffffffffL,
            0); // constant part
        
        return uuid;
    }
}
