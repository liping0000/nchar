package net.wohlfart.authorization;

import static org.jboss.seam.ScopeType.STATELESS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;

import org.apache.commons.lang.StringUtils;
import org.jasypt.util.password.ConfigurablePasswordEncryptor;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.security.management.PasswordHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is responsible for hashing the passwords in the database password
 * field, we use a tag in the password to give a hint for the used password
 * algorithm, this also means we should not use the "{" and "}" character in the
 * password because the user might accidently use a password with a valid tag
 * for one of the used algorithms
 * 
 */

@Scope(STATELESS)
@Name("org.jboss.seam.security.passwordHash")
@Install(precedence = Install.APPLICATION)
@BypassInterceptors
public class CustomHash extends PasswordHash {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomHash.class);
    
    // there are more in the super class
    public static final String ALGORITHM_SSHA = "SSHA";                                 
    public static final String ALGORITHM_PLAIN = "PLAIN";                                 

       
    // needed for tests
    public static final String   BEGIN_TAG_SHA         = "{SHA}";
    public static final String   BEGIN_TAG_SSHA        = "{SSHA}";
    public static final String   BEGIN_TAG_MD5         = "{MD5}";
    public static final String   BEGIN_TAG_PLAIN       = "{PLAIN}";
    

   
    private enum HashAlgorithm {
        SHA(PasswordHash.ALGORITHM_SHA, "SHA-1", BEGIN_TAG_SHA),
        SSHA(ALGORITHM_SSHA, "SHA-1", BEGIN_TAG_SSHA),
        MD5(PasswordHash.ALGORITHM_MD5, "MD5", BEGIN_TAG_MD5),
        PLAIN(ALGORITHM_PLAIN, null, BEGIN_TAG_PLAIN),
        none("none", "", "");
        
        protected final String name;
        protected final String digest;
        protected final String tag;
         
        HashAlgorithm(String name, String digest, String tag) {
            this.name = name;
            this.digest = digest;
            this.tag = tag;
        }
        
        public static HashAlgorithm find(String name) {
            for (HashAlgorithm alg : HashAlgorithm.values()) {
                if (alg.name.equals(name)) {
                    return alg;
                }
            }
            return null;
        }
    }

    
    // internal setting for hashing:
    // do not use iterated hashing! same password may return different values on
    // multiple calls
    // FIXME: we might be able to use this now since we changed client code to
    // use
    // checkPassword() only
    // not used yet
    public static final int      ITERATIONS            = 0;                                       
    
    // plain (true) means no use of salt and no iterated hashing
    private static final Boolean IS_PLAIN_DIGEST       = true;                                     
    
    // alternative we can use "hexadecimal", "base64" is used in ldap
    private static final String  STRING_OUTPUT_TYPE    = "base64";


    public static CustomHash instance() {
        return (CustomHash) Component.getInstance(CustomHash.class, ScopeType.STATELESS);
    }

    public String generateSaltedHash(final String password, final CharmsUser user) {
        // FIXME: username can be changed, so the username is no secure salt value
        // return generateSaltedHash(password, user.getName(), CharmsUser.PASSWORD_HASH_FUNCTION);
        return generateSaltedHash(password, "", CharmsUser.PASSWORD_HASH_FUNCTION);
    }

    @Override
    public String generateSaltedHash(final String password, final String saltPhrase) {
        return generateSaltedHash(password, saltPhrase, CharmsUser.PASSWORD_HASH_FUNCTION);
    }

    @Override
    public String generateSaltedHash(
            final String password, 
            final String saltPhrase, 
            final String algorithmName) {
        // seam uses the plain username as salt
        // passwd: passwd1 saltPhrase: username algorithm: SSHA
        try {
            // "none" means we don't use encryption for passwords
            HashAlgorithm hashAlgorithm = HashAlgorithm.find(algorithmName);           
            if (HashAlgorithm.none.equals(hashAlgorithm)) {
                LOGGER.warn("using no password encryption");
                return password;
            } else if (hashAlgorithm != null) {
                final ConfigurablePasswordEncryptor encryptor = new ConfigurablePasswordEncryptor();
                encryptor.setAlgorithm(hashAlgorithm.digest);
                encryptor.setPlainDigest(IS_PLAIN_DIGEST);
                encryptor.setStringOutputType(STRING_OUTPUT_TYPE);
                LOGGER.info("using {} for password encryption", hashAlgorithm.digest);
                // NOTE: we are NOT using the salt here to encrypt the password
                // and no iterations either
                //
                // the encypt function somehow adds linefeed to the password,
                // we have to trim to avoid that
                final String result = hashAlgorithm.tag + encryptor.encryptPassword(password).trim();
                return result;
            } else {
                LOGGER.warn("using unknown password algorithm, calling super class to encrypt: {}", algorithmName);
                return super.createPasswordKey(password.toCharArray(), saltPhrase.getBytes(), ITERATIONS);
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean checkPassword(final String plainPassword, final CharmsUser user) {
        // FIXME: username can be changed, so this is no secure salt value
        return checkPassword(plainPassword, user.getName(), user.getPasswd(), CharmsUser.PASSWORD_HASH_FUNCTION);
    }

    public boolean checkPassword(final String plainPassword, final String saltPhrase, final String encryptedPassword) {
        return checkPassword(plainPassword, saltPhrase, encryptedPassword, CharmsUser.PASSWORD_HASH_FUNCTION);
    }

    private boolean checkPassword(
            final String plainPassword, 
            final String saltPhrase, 
            final String encryptedPassword, 
            final String algorithmName) {
        try {
            // "none" means we don't use encryption for passwords
            HashAlgorithm hashAlgorithm = HashAlgorithm.find(algorithmName);           
            if (HashAlgorithm.none.equals(hashAlgorithm)) {
                return StringUtils.equals(plainPassword, encryptedPassword);
            }  else if (hashAlgorithm != null) {
                
                // the real encryption happens here:
                if (isPasswordEncryptedProperly(encryptedPassword, algorithmName)) {
                    // chop off the tag
                    final String hashWithoutTag = StringUtils.substring(encryptedPassword, hashAlgorithm.tag.length());
                    // FIXME: this seems to work too, check why:
                    // hashWithoutTag = StringUtils.substring(encryptedPassword,
                    // BEGIN_TAG.length() - 1);
                    final ConfigurablePasswordEncryptor encryptor = new ConfigurablePasswordEncryptor();
                    encryptor.setAlgorithm(hashAlgorithm.digest);
                    encryptor.setPlainDigest(IS_PLAIN_DIGEST);
                    encryptor.setStringOutputType(STRING_OUTPUT_TYPE);
                    LOGGER.debug("using {} for password encryption", hashAlgorithm.digest);
                    return encryptor.checkPassword(plainPassword, hashWithoutTag);
                    
                } else {
                    // we have a defined encryption algorithm but the password
                    // doesn't start with
                    // the proper tag there seems to be a problem
                    LOGGER.warn("we are using {} to encrypt passwords but the password doesn't start with the proper tag {}", 
                            hashAlgorithm.name, hashAlgorithm.tag);
                    return false;
                }
            } else {
                LOGGER.warn("using unknown password algorithm: {}", algorithmName);
                final String pass = super.createPasswordKey(plainPassword.toCharArray(), saltPhrase.getBytes(), ITERATIONS);
                StringUtils.equals(pass, encryptedPassword);
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }

    public boolean isPasswordEncryptedProperly(final String encryptedPassword) {
        return isPasswordEncryptedProperly(encryptedPassword, CharmsUser.PASSWORD_HASH_FUNCTION);
    }

    public boolean isPasswordEncryptedProperly(
            final String encryptedPassword, 
            final String algorithmName) {
        
        HashAlgorithm hashAlgorithm = HashAlgorithm.find(algorithmName);
        if (hashAlgorithm == null) {
            LOGGER.warn("unable to find encryption algorithm for {}", algorithmName);
            return false;
        }
        return encryptedPassword.startsWith(hashAlgorithm.tag);
    }


}
