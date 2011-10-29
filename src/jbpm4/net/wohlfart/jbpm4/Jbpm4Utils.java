package net.wohlfart.jbpm4;

import java.io.InputStream;
import java.util.List;

import net.wohlfart.BootSequence;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Jbpm4Utils {

    private final static Logger LOGGER = LoggerFactory.getLogger(BootSequence.class);

    
    public static String findProcessName(final InputStream inputStream) throws DocumentException {
        final Document document = new SAXReader().read(inputStream);
        // find the name attribute of the process node
        final List<?> list = document.selectNodes("//process[@name]");
        LOGGER.debug("selected list is {}", list);
        if (list.size() != 1) { // there must be exactly one process definition node in the xml...
            LOGGER.warn("found nonuniqe process definition names, count is {}", list.size());
            return null;
        }
        LOGGER.debug("selected element is {} is type: {}", list.get(0), list.get(0).getClass());
        final DefaultElement element = ((DefaultElement) list.get(0));
        final String name = element.attribute("name").getValue();
        return name;
    }


    public static String extractFileNameWithSuffix(final String filePathName) {
        if (filePathName == null) {
            return null;
        }

        final int pos1 = filePathName.lastIndexOf('\\');
        final int pos2 = filePathName.lastIndexOf('/');

        final int pos = Math.max(pos1, pos2);
        if (pos > 0) {
            return filePathName.substring(pos + 1);
        } else {
            return filePathName;
        }
    }

}
