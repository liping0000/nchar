package net.wohlfart.framework.excel;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authentication.entities.Gender;
import net.wohlfart.authorization.CustomHash;
import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.email.entities.CharmsEmailTemplateReceiver;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Session;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class to deploy the reference data * produtcs * parts * errors * ...
 * 
 * @author Michael Wohlfart
 * 
 */
public class WorkbookDeployer implements Serializable {


    private final static Logger LOGGER                    = LoggerFactory.getLogger(WorkbookDeployer.class);

    // manually injected
    private Session             hibernateSession;

    // sheetnames for the import
    private static final String PRODUCTS_SHEET_NAME       = "Maschinentypen";
    private static final String PARTS_SHEET_NAME          = "Bereiche";
    private static final String ERRORS_SHEET_NAME         = "Codierungen";

    private static final String REFERENCE_SHEET_NAME      = "Zuordnungen";

    private static final String USER_SHEET_NAME           = "Benutzerdaten";

    private static final String EMAIL_TEMPLATE_SHEET_NAME = "Emailvorlagen";

    /**
     * split up the input stream into worksheets and dispatch the import
     * according to the the object type
     * 
     * @param inputStream
     * @throws IOException
     */
    @Transactional
    // probably doesn't do anything since we don't keep this deployer in a seam
    // context...
    public void deployData(final HSSFWorkbook workbook, final Session hibernateSession) throws IOException {
        this.hibernateSession = hibernateSession;

        // loop through the sheets..
        final int sheetCount = workbook.getNumberOfSheets();
        LOGGER.debug("sheetCount is: {} ...", sheetCount);
        for (int index = 0; index < sheetCount; index++) {
            final String name = workbook.getSheetName(index);
            LOGGER.debug("loading sheet {} ...", name);
            final HSSFSheet sheet = workbook.getSheetAt(index);

            // FIXME: order of the sheets is critical

            // FIXME: make a second parsing run for the translations

            // different imports according to the sheet names...
            if (PRODUCTS_SHEET_NAME.equals(name)) {
                deployProductData(sheet);
            } else if (PARTS_SHEET_NAME.equals(name)) {
                deployPartData(sheet);
            } else if (ERRORS_SHEET_NAME.equals(name)) {
                deployErrorData(sheet);
            } else if (REFERENCE_SHEET_NAME.equals(name)) {
                deployReferences(sheet);
            } else if (USER_SHEET_NAME.equals(name)) {
                deployUserData(sheet);
            } else if (EMAIL_TEMPLATE_SHEET_NAME.equals(name)) {
                deployEmailTemplates(sheet);
            }

            // flush for good measure
            hibernateSession.flush();
        }
    }

    private void deployProductData(final HSSFSheet sheet) {
        final String NAME = "name";

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { NAME });
        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();
            final String name = map.get(NAME);

            final ChangeRequestProduct product = new ChangeRequestProduct();
            product.setDefaultName(name);
            addNonexistent(product);
        }
    }

    /**
     * handle a single sheet with part data
     * 
     * @param sheet
     */
    private void deployPartData(final HSSFSheet sheet) {
        final String NAME = "name";

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { NAME });
        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();
            final String name = map.get(NAME);

            final ChangeRequestUnit part = new ChangeRequestUnit();
            part.setDefaultName(name);
            addNonexistent(part);
        }
    }

    /**
     * handle a single sheet with error data
     * 
     * @param sheet
     */
    private void deployErrorData(final HSSFSheet sheet) {
        final String NAME = "name";

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { NAME });
        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();
            final String name = map.get(NAME);

            final ChangeRequestCode code = new ChangeRequestCode();
            code.setDefaultName(name);
            addNonexistent(code);
        }
    }

    /**
     * handle a single sheet with product data
     * 
     * @param sheet
     */
    private void deployReferences(final HSSFSheet sheet) {
        final String PRODUCT = "product";
        final String UNIT = "unit";
        final String CODE = "code";

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { PRODUCT, UNIT, CODE });
        ChangeRequestProduct product = null;
        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();
            final String productName = map.get(PRODUCT);
            final String unitName = map.get(UNIT);
            final String codeName = map.get(CODE);

            // do we need to set up a new product
            if (productName != null) {
                product = new ChangeRequestProduct();
                product.setDefaultName(productName);
                product = addNonexistent(product);
            } // else we continue with the old

            // is there a part to add
            if (unitName != null) {
                ChangeRequestUnit part = new ChangeRequestUnit();
                part.setDefaultName(unitName);
                part = addNonexistent(part);
                if (product != null) {
                    product.getUnits().add(part);
                    hibernateSession.persist(product);
                } else {
                    LOGGER.warn("product is null");
                }
            }

            // is there an error to add
            if (codeName != null) {
                ChangeRequestCode code = new ChangeRequestCode();
                code.setDefaultName(codeName);
                code = addNonexistent(code);
                if (product != null) {
                    product.getCodes().add(code);
                    hibernateSession.persist(product);
                } else {
                    LOGGER.warn("error is null");
                }
            }
            hibernateSession.flush();
        }
    }

    private void deployEmailTemplates(final HSSFSheet sheet) {
        final String NAME = "name";
        final String SENDER_EXPRESSION = "senderExpression";
        final String RECEIVER_EXPRESSION = "receiverExpression";
        final String SUBJECT = "subject";
        final String BODY = "body";

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { NAME, RECEIVER_EXPRESSION, SUBJECT, BODY });
        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();

            CharmsEmailTemplate template = new CharmsEmailTemplate();

            final String name = (map.get(NAME) == null) ? "" : map.get(NAME).trim();
            final String subject = (map.get(SUBJECT) == null) ? "" : map.get(SUBJECT).trim();
            final String body = (map.get(BODY) == null) ? "" : map.get(BODY).trim();
            final String receiverExpr = (map.get(RECEIVER_EXPRESSION) == null) ? "" : map.get(RECEIVER_EXPRESSION).trim();
            final String senderExpr = (map.get(SENDER_EXPRESSION) == null) ? "" : map.get(SENDER_EXPRESSION).trim();

            template.setName(name);
            template.setSubject(subject);
            template.setBody(body);
            template.setSender(senderExpr);
            template.setLastModified(Calendar.getInstance().getTime());
            template.setEnabled(true); // enable by default

            template = addNonexistent(template);
            // override if we already got this template
            template.setSubject(subject);
            template.setBody(body);
            template.setSender(senderExpr);
            template.setLastModified(Calendar.getInstance().getTime());

            final ArrayList<CharmsEmailTemplateReceiver> list = new ArrayList<CharmsEmailTemplateReceiver>();
            final CharmsEmailTemplateReceiver receiver = new CharmsEmailTemplateReceiver();
            receiver.setAddressExpression(receiverExpr);
            receiver.setTemplate(template);
            template.setReceiver(list);

            hibernateSession.persist(template);
            hibernateSession.persist(receiver);
            hibernateSession.flush();
        }

    }

    private void deployUserData(final HSSFSheet sheet) {
        final String FIRSTNAME = "firstname";
        final String LASTNAME = "lastname";
        // final String LOGIN = "login";
        // final String PASSWORD = "password";
        final String EMAIL = "email";
        final String GENDER = "gender";
        final String ENABLED = "enabled";
        final String EXTERNAL_ID1 = "externalId1"; // Kostenstelle
        final String EXTERNAL_ID2 = "externalId2"; // Personalnummer

        final DataSheetIterator iterator = new DataSheetIterator(sheet, new String[] { FIRSTNAME, LASTNAME, EMAIL, GENDER,
                // LOGIN,
                // PASSWORD,
                ENABLED, EXTERNAL_ID1, EXTERNAL_ID2 });

        while (iterator.hasNext()) {
            final HashMap<String, String> map = iterator.next();

            final String login = map.get(EXTERNAL_ID2); // PNR, Login, Passwd

            // make sure we don't submit the same PNR twice:
            final int count = hibernateSession.getNamedQuery(CharmsUser.FIND_BY_NAME).setParameter("name", login).list().size();

            if (count == 0) {

                final CharmsUser user = new CharmsUser();

                user.setFirstname(map.get(FIRSTNAME));
                user.setLastname(map.get(LASTNAME));
                user.setEmail(map.get(EMAIL));

                final String gender = map.get(GENDER);
                if ((gender != null) && (gender.equalsIgnoreCase("M") || gender.equalsIgnoreCase("male"))) {
                    user.setGender(Gender.MALE);
                }
                if ((gender != null) && (gender.equalsIgnoreCase("F") || gender.equalsIgnoreCase("female"))) {
                    user.setGender(Gender.FEMALE);
                }

                final String enabled = map.get(ENABLED);
                if ((enabled != null) && (enabled.equalsIgnoreCase("Y") || enabled.equalsIgnoreCase("T"))) {
                    user.setEnabled(true);
                }
                if ((enabled != null) && (enabled.equalsIgnoreCase("N") || enabled.equalsIgnoreCase("F"))) {
                    user.setEnabled(false);
                }

                user.setExternalId1(map.get(EXTERNAL_ID1));
                user.setExternalId2(map.get(EXTERNAL_ID2));

                // FIXME: use a default name if external id is null
                final String username = map.get(EXTERNAL_ID2); // PNR
                user.setName(login);

                String passwd = map.get(EXTERNAL_ID2);
                if ((passwd == null) || (passwd.length() < 3)) {
                    passwd += passwd;
                    passwd += passwd;
                } else if (passwd.length() < 6) {
                    // passwd += passwd;
                }
                user.setPasswd(CustomHash.instance().generateSaltedHash(passwd, username));
                user.setLocaleId("de");

                hibernateSession.persist(user);
                hibernateSession.flush();
            } // end count == 0
        }
    }

    // --------------- add single objects to the database ----------------

    private CharmsEmailTemplate addNonexistent(final CharmsEmailTemplate template) {
        // FIXME: we can have multiple templates with the same name
        final int count = hibernateSession
            .getNamedQuery(CharmsEmailTemplate.FIND_BY_NAME)
            .setParameter("name", template.getName()) 
            .list()
            .size();
        
        if (count == 0) {
            hibernateSession.persist(template);
            hibernateSession.flush();
        }

        return (CharmsEmailTemplate) hibernateSession
            .getNamedQuery(CharmsEmailTemplate.FIND_BY_NAME)
            .setParameter("name", template.getName())
            .uniqueResult();
    }

    private ChangeRequestUnit addNonexistent(final ChangeRequestUnit part) {
        final int count = hibernateSession
            .getNamedQuery(ChangeRequestUnit.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", part.getDefaultName())
            .list()
            .size();
        
        if (count == 0) {
            part.setupSortIndex(hibernateSession);
            hibernateSession.persist(part);
            hibernateSession.flush();
        }

        return (ChangeRequestUnit) hibernateSession
            .getNamedQuery(ChangeRequestUnit.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", part.getDefaultName())
            .uniqueResult();
    }

    private ChangeRequestCode addNonexistent(final ChangeRequestCode code) {
        final int count = hibernateSession
            .getNamedQuery(ChangeRequestCode.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", code.getDefaultName())
            .list()
            .size();

        if (count == 0) {
            code.setupSortIndex(hibernateSession);
            hibernateSession.persist(code);
            hibernateSession.flush();
        }

        return (ChangeRequestCode) hibernateSession
            .getNamedQuery(ChangeRequestCode.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", code.getDefaultName())
            .uniqueResult();
    }

    private ChangeRequestProduct addNonexistent(final ChangeRequestProduct product) {
        final int count = hibernateSession
            .getNamedQuery(ChangeRequestProduct.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", product.getDefaultName())
            .list()
            .size();

        if (count == 0) {
            product.setupSortIndex(hibernateSession);
            hibernateSession.persist(product);
            hibernateSession.flush();
        }

        return (ChangeRequestProduct) hibernateSession
            .getNamedQuery(ChangeRequestProduct.FIND_BY_DEFAULT_NAME)
            .setParameter("defaultName", product.getDefaultName())
            .uniqueResult();
    }

}
