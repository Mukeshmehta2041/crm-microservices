package com.crm.platform.security.encryption.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields for automatic encryption/decryption
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    
    /**
     * Whether to encrypt null values (default: false)
     */
    boolean encryptNulls() default false;
    
    /**
     * Custom encryption algorithm (default: uses service default)
     */
    String algorithm() default "";
}