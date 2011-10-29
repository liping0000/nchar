package net.wohlfart.email.freemarker;

import java.io.Reader;
import java.io.StringReader;
import java.util.Date;

import net.wohlfart.email.entities.CharmsEmailTemplate;

/**
 * just wrapping a reader around the template text
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public class MailTemplateSource {

    private final StringBuilder fullText     = new StringBuilder();
    private Date                lastModified = null;

    // create the fallback template if we don't have a translation
    public MailTemplateSource(final CharmsEmailTemplate template) {
        lastModified = template.getLastModified();
        fullText.append(template.getBody());
    }

    // create a translated template since we have a translation for lookupLocale
    // the caller is responsible for checking the availability of the
    // translation
    public MailTemplateSource(final CharmsEmailTemplate template, final String lookupLocale) {
        lastModified = template.getLastModified();
        fullText.append(template.getTranslations().get(lookupLocale).getBody());
    }

    protected Date getLastModified() {
        return lastModified;
    }

    protected Reader getReader() {
        final StringReader reader = new StringReader(fullText.toString());
        return reader;
    }
}
