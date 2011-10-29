package net.wohlfart.framework.search;

import net.wohlfart.framework.i18n.ITranslateable;

import org.hibernate.search.bridge.StringBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslatableReferenceBridge implements StringBridge {

    private final static Logger LOGGER = LoggerFactory.getLogger(TranslatableReferenceBridge.class);

    @Override
    public String objectToString(final Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof ITranslateable) {
            final ITranslateable translateable = (ITranslateable) object;
            return translateable.getId().toString();
        } else {
            LOGGER.warn("wrong object type in Search Bridge: {}, is not null and should be ITranslateable", object.getClass());
            return null;
        }

    }

}
