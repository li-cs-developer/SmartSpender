package com.smartspender.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Dev-only utility. Prints the BCrypt hash of a plaintext password.
 *
 * Usage:
 *   mvn -q compile exec:java -Dexec.mainClass="com.smartspender.util.GenerateHash" -Dexec.args="hunter2hunter2"
 *
 * Keep this around — useful whenever you need to seed a password in SQL.
 */
public class GenerateHash {
    public static void main(String[] args) {
        String plain = args.length > 0 ? args[0] : "hunter2hunter2";
        String hash = new BCryptPasswordEncoder().encode(plain);
        System.out.println("Plain: " + plain);
        System.out.println("Hash:  " + hash);
    }
}