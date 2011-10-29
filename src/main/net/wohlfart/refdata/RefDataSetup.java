package net.wohlfart.refdata;

import static org.jboss.seam.ScopeType.STATELESS;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;

import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * locales are configured in components.xml see:
 * https://cloud.prod.atl2.jboss.com:8443/jira/browse/JBSEAM-3088
 * 
 * @author Michael Wohlfart
 * 
 */
@Scope(STATELESS)
@Name("refDataSetup")
public class RefDataSetup {

    private final static Logger LOGGER = LoggerFactory.getLogger(RefDataSetup.class);

    // @In(create=true)
    // EntityManager entityManager;
    @In(value = "hibernateSession")
    private Session             hibernateSession;

    // There is no FacesContext available during application startup.
    // this happens before JSF init! no way to read the supported languages
    // from the faces context
    @Transactional
    public void startup() {
        LOGGER.debug("running startup for reference data, initializing dummy values for testing");

        // add some machines:
        final ChangeRequestProduct product1 = new ChangeRequestProduct();
        product1.setDefaultName("Maschinenetyp1");
        addNonexistent(product1);

        final ChangeRequestProduct product2 = new ChangeRequestProduct();
        product2.setDefaultName("Maschinenetyp2");
        addNonexistent(product2);

        final ChangeRequestProduct product3 = new ChangeRequestProduct();
        product3.setDefaultName("Maschinenetyp3");
        addNonexistent(product3);

        // add some parts:
        final ChangeRequestUnit part1 = new ChangeRequestUnit();
        part1.setDefaultName("Part1");
        addNonexistent(part1);

        final ChangeRequestUnit part2 = new ChangeRequestUnit();
        part2.setDefaultName("Part2");
        addNonexistent(part2);

        final ChangeRequestUnit part3 = new ChangeRequestUnit();
        part3.setDefaultName("Part3");
        addNonexistent(part3);

        // add some errors:
        final ChangeRequestCode code1 = new ChangeRequestCode();
        code1.setDefaultName("Error1");
        addNonexistent(code1);

        final ChangeRequestCode code2 = new ChangeRequestCode();
        code2.setDefaultName("Error2");
        addNonexistent(code2);

        final ChangeRequestCode code3 = new ChangeRequestCode();
        code3.setDefaultName("Error3");
        addNonexistent(code3);

    }

    private void addNonexistent(final ChangeRequestUnit part) {
        final int count = hibernateSession.getNamedQuery(ChangeRequestUnit.FIND_BY_DEFAULT_NAME).setParameter("defaultName", part.getDefaultName()).list()
                .size();

        if (count == 0) {
            part.setupSortIndex(hibernateSession);
            hibernateSession.persist(part);
            hibernateSession.flush();
        }
    }

    private void addNonexistent(final ChangeRequestProduct product) {
        final int count = hibernateSession.getNamedQuery(ChangeRequestProduct.FIND_BY_DEFAULT_NAME).setParameter("defaultName", product.getDefaultName())
                .list().size();

        if (count == 0) {
            product.setupSortIndex(hibernateSession);
            hibernateSession.persist(product);
            hibernateSession.flush();
        }
    }

    private void addNonexistent(final ChangeRequestCode code) {
        final int count = hibernateSession.getNamedQuery(ChangeRequestCode.FIND_BY_DEFAULT_NAME).setParameter("defaultName", code.getDefaultName()).list()
                .size();

        if (count == 0) {
            code.setupSortIndex(hibernateSession);
            hibernateSession.persist(code);
            hibernateSession.flush();
        }
    }

}
