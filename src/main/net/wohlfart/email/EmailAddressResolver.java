package net.wohlfart.email;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.wohlfart.authentication.entities.CharmsUser;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class detects valid email expressions for resolving email tokens in the
 * sender/receiver field of an email template
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class EmailAddressResolver implements IAddressTokenResolver {

    private final static Logger LOGGER        = LoggerFactory.getLogger(EmailAddressResolver.class);

    // a pattern for valid email expressions
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*"
                                                      + "@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)");

    @Override
    public Set<CharmsUser> resolve(final String expression, final ExecutionImpl executionImpl, final Session session) {

        // check for a fixed email address
        if (StringUtils.isNotEmpty(expression) && EMAIL_PATTERN.matcher(expression).matches()) {
            // create a temp user since we just have an email address
            final Set<CharmsUser> set = new HashSet<CharmsUser>();
            final CharmsUser user = new CharmsUser();
            user.setEmail(expression);
            set.add(user);
            LOGGER.debug("resolved email address: " + user);
            return set;
        }

        return null;
    }

}
