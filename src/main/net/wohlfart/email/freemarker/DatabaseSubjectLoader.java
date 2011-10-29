package net.wohlfart.email.freemarker;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

import net.wohlfart.email.entities.CharmsEmailTemplate;

import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;

/**
 * this class is for enhancing the freemarker config
 * 
 * @author Michael Wohlfart
 */
public class DatabaseSubjectLoader {

    private final static Logger LOGGER = LoggerFactory.getLogger(DatabaseSubjectLoader.class);

    @SuppressWarnings("unchecked")
    public Template getSubjectAsTemplate(final String templateId, final Locale locale, final ExecutionImpl execution, final Session session,
            final MailConfiguration mailConfiguration) throws IOException {

        // default
        String subjectString = "subject";

        // FIXME: add localization
        final List<String> subjects = session
            .getNamedQuery(CharmsEmailTemplate.FIND_SUBJECT_BY_ID)
            .setParameter("id", new Long(templateId))
            .list();

        if (subjects.size() == 1) {
            subjectString = subjects.get(0);
        } else {
            LOGGER.warn("can't resolve subject for mailtemplate id {} ", templateId);
        }

        // just a dummy name
        return new Template(templateId + "_subject" + locale, new StringReader(subjectString), mailConfiguration);
    }

}
