package org.glygen.array.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordGeneratorUtil {

	public static void main(String[] args) {
		
		if (args.length > 1 && args[1].equals("DB")) {
			
			// use jasypt to encrypt the password
			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			// the following needs to match with the ones in application context and environment variable setting
			String password = System.getenv("JASYPT_SECRET");
			encryptor.setPassword(password);                     // we HAVE TO set a password
			encryptor.setAlgorithm("PBEWithMD5AndDES");          // set the algorithm
			String encryptedText = encryptor.encrypt(args[0]);
			System.out.println ("encyrpted password: " + encryptedText);
		}
		else {
			PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
			String hashedPassword = passwordEncoder.encode(args[0]);
			System.out.println ("encyrpted password: " + hashedPassword);
		}
	}
}
