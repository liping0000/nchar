package net.wohlfart.jbpm4.binding;

import net.wohlfart.jbpm4.mail.CustomMailProducer;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.activity.MailActivity;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.wire.descriptor.ObjectDescriptor;
import org.jbpm.pvm.internal.wire.descriptor.ProvidedObjectDescriptor;
import org.jbpm.pvm.internal.wire.usercode.UserCodeReference;
import org.jbpm.pvm.internal.xml.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * our custom mail binding in order to have our custom mail producer instead of
 * the default one, not this can also be achieved by using the class attribute
 * in the mail node however, having a custom binding simplifies the workflow
 * definition...
 * 
 */
public class CustomMailBinding extends JpdlBinding {

    private final static Logger LOGGER = LoggerFactory.getLogger(TransitionConfigBinding.class);

    private static final String TAG    = "customMail";

    public CustomMailBinding() {
        super(TAG);
        LOGGER.debug("constructor finished");
    }

    @Override
    public Object parseJpdl(final Element element, final Parse parse, final JpdlParser parser) {
        final MailActivity activity = new MailActivity();

        final ObjectDescriptor descriptor = new ObjectDescriptor(CustomMailProducer.class);

        final String templateName = XmlUtil.attribute(element, // element
                "templateName", // attribute name
                // true, // required, we need a name
                parse, // parse run
                null); // default value

        descriptor.addInjection("templateName", new ProvidedObjectDescriptor(templateName));
        final UserCodeReference userCodeReference = new UserCodeReference();
        userCodeReference.setDescriptor(descriptor);
        activity.setMailProducerReference(userCodeReference);
        return activity;
    }
}
