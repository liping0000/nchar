package net.wohlfart.charms.test.action;

import java.util.List;

import net.wohlfart.changerequest.ChangeRequestCodeHome;
import net.wohlfart.changerequest.ChangeRequestProductHome;
import net.wohlfart.changerequest.ChangeRequestUnitHome;
import net.wohlfart.refdata.entities.ChangeRequestCode;
import net.wohlfart.refdata.entities.ChangeRequestCodeItem;
import net.wohlfart.refdata.entities.ChangeRequestProduct;
import net.wohlfart.refdata.entities.ChangeRequestUnit;
import net.wohlfart.refdata.entities.ChangeRequestUnitItem;

import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProductReferenceActionBeanTest extends AbstractHomeActionBase {

    ChangeRequestProductHome productEntityHome;
    ChangeRequestUnitHome    unitEntityHome;
    ChangeRequestCodeHome    codeEntityHome;

    @BeforeMethod
    @Override
    public void begin() {
        super.begin();
        // setup the component under test
        productEntityHome = (ChangeRequestProductHome) Component.getInstance("changeRequestProductHome");
        unitEntityHome = (ChangeRequestUnitHome) Component.getInstance("changeRequestUnitHome");
        codeEntityHome = (ChangeRequestCodeHome) Component.getInstance("changeRequestCodeHome");
        Assert.assertNotNull(productEntityHome, "productEntityHome is null");
        Assert.assertNotNull(unitEntityHome, "unitEntityHome is null");
        Assert.assertNotNull(codeEntityHome, "codeEntityHome is null");
    }
    
    @Test
    public void testNameCollision() {
        String outcome;

        ChangeRequestProduct changeRequestProduct;
        productEntityHome.clearInstance();
        outcome = productEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid");
        changeRequestProduct = productEntityHome.getInstance();
        Assert.assertNotNull(changeRequestProduct);
        changeRequestProduct.setDefaultName("name1");
        changeRequestProduct.setEnabled(true);
        outcome = productEntityHome.persist();
        Long id = changeRequestProduct.getId();
        Assert.assertEquals(outcome, "persisted");
        hibernateSession.flush();
       
        productEntityHome.clearInstance();
        outcome = productEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid");
        changeRequestProduct = productEntityHome.getInstance();
        Assert.assertNotNull(changeRequestProduct);
        changeRequestProduct.setDefaultName("name1");
        changeRequestProduct.setEnabled(true);
        outcome = productEntityHome.persist();
        Assert.assertEquals(outcome, "invalid");
        hibernateSession.flush();
        hibernateSession.clear();
        
        changeRequestProduct = (ChangeRequestProduct) hibernateSession.get(ChangeRequestProduct.class, id);
        hibernateSession.delete(changeRequestProduct);
      
        hibernateSession.flush();
        hibernateSession.clear();        
    }

    @Test
    public void testAssignAction() {
        String outcome;

        // /// creating products
//        try {
//            Thread.sleep(100 * 60 * 1000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        // create products
         ChangeRequestProduct product1 = createProduct("productRefTest1");
        Assert.assertNotNull(product1);
         ChangeRequestProduct product2 = createProduct("productRefTest2");
        Assert.assertNotNull(product2);
         ChangeRequestProduct product3 = createProduct("productRefTest3");
        Assert.assertNotNull(product3);
//        final ChangeRequestProduct product4 = createProduct("productRefTest3");
//        Assert.assertNotNull(product4);
        // create units
         ChangeRequestUnit unit1 = createUnit("unitRefTest1");
        Assert.assertNotNull(unit1);
         ChangeRequestUnit unit2 = createUnit("unitRefTest2");
        Assert.assertNotNull(unit2);
         ChangeRequestUnit unit3 = createUnit("unitRefTest3");
        Assert.assertNotNull(unit3);
        // create codes
         ChangeRequestCode code1 = createCode("codeRefTest1");
        Assert.assertNotNull(code1);
         ChangeRequestCode code2 = createCode("codeRefTest2");
        Assert.assertNotNull(code2);
         ChangeRequestCode code3 = createCode("codeRefTest3");
        Assert.assertNotNull(code3);

        hibernateSession.flush();

        // /// attach all codes and units to the product1
        Long id1 = product1.getId();
        outcome = productEntityHome.setEntityId(id1 + "");
        Assert.assertEquals(outcome, "valid");
        outcome = productEntityHome.getChangeRequestProduct().getDefaultName();
        Assert.assertEquals(outcome, "productRefTest1");

        List<ChangeRequestCodeItem> selectedCodeItems1 = productEntityHome.getSelectedCodeItems();
        Assert.assertEquals(selectedCodeItems1.size(), 0);
        List<ChangeRequestUnitItem> selectedUnitItems1 = productEntityHome.getSelectedUnitItems();
        Assert.assertEquals(selectedUnitItems1.size(), 0);

        // move all available to the selected list
        final List<ChangeRequestCodeItem> availableCodes1 = productEntityHome.getAvailableCodeItems();
        Assert.assertTrue(availableCodes1.size() > 0, "persisted codes not available as selection");
        final List<ChangeRequestUnitItem> availableUnits1 = productEntityHome.getAvailableUnitItems();
        Assert.assertTrue(availableUnits1.size() > 0, "persisted units not available as selection");
        productEntityHome.setSelectedCodeItems(availableCodes1);
        productEntityHome.setSelectedUnitItems(availableUnits1);

        selectedCodeItems1 = productEntityHome.getSelectedCodeItems();
        final int codeCount1 = selectedCodeItems1.size();
        Assert.assertTrue(codeCount1 > 0, "persisted codes not available as selection");
        selectedUnitItems1 = productEntityHome.getSelectedUnitItems();
        final int unitCount1 = selectedUnitItems1.size();
        Assert.assertTrue(unitCount1 > 0, "persisted units not available as selection");

        outcome = productEntityHome.update();
        Assert.assertEquals(outcome, "updated");

        // /// check product2
        final Long id2 = product2.getId();
        outcome = productEntityHome.setEntityId(id2 + "");
        Assert.assertEquals(outcome, "valid");
        outcome = productEntityHome.getChangeRequestProduct().getDefaultName();
        Assert.assertEquals(outcome, "productRefTest2");

        final List<ChangeRequestCodeItem> selectedCodeItems2 = productEntityHome.getSelectedCodeItems();
        Assert.assertEquals(selectedCodeItems2.size(), 0);
        final List<ChangeRequestUnitItem> selectedUnitItems2 = productEntityHome.getSelectedUnitItems();
        Assert.assertEquals(selectedUnitItems2.size(), 0);

        // go back to product1
        id1 = product1.getId();
        outcome = productEntityHome.setEntityId(id1 + "");
        Assert.assertEquals(outcome, "valid");
        outcome = productEntityHome.getChangeRequestProduct().getDefaultName();
        Assert.assertEquals(outcome, "productRefTest1");

        // check if they were all persisted
        Assert.assertEquals(productEntityHome.getSelectedCodeItems().size(), codeCount1);
        Assert.assertEquals(productEntityHome.getSelectedUnitItems().size(), unitCount1);
        // and none available for selection
        Assert.assertEquals(productEntityHome.getAvailableCodeItems().size(), 0);
        Assert.assertEquals(productEntityHome.getAvailableUnitItems().size(), 0);

        // now remove one on each list to trigger reordering/resorting
        final List<ChangeRequestCodeItem> codes = productEntityHome.getSelectedCodeItems();
        codes.remove(0);
        productEntityHome.setSelectedCodeItems(codes);

        final List<ChangeRequestUnitItem> units = productEntityHome.getSelectedUnitItems();
        units.remove(0);
        productEntityHome.setSelectedUnitItems(units);

        outcome = productEntityHome.update();
        Assert.assertEquals(outcome, "updated");

        // go back to product2
        id1 = product1.getId();
        outcome = productEntityHome.setEntityId(id2 + "");
        Assert.assertEquals(outcome, "valid");
        outcome = productEntityHome.getChangeRequestProduct().getDefaultName();
        Assert.assertEquals(outcome, "productRefTest2");

        Assert.assertEquals(productEntityHome.getSelectedCodeItems().size(), 0);
        Assert.assertEquals(productEntityHome.getSelectedUnitItems().size(), 0);
        Assert.assertEquals(productEntityHome.getAvailableCodeItems().size(), codeCount1);
        Assert.assertEquals(productEntityHome.getAvailableUnitItems().size(), unitCount1);

        // now check if product1 is alright
        id1 = product1.getId();
        outcome = productEntityHome.setEntityId(id1 + "");
        Assert.assertEquals(outcome, "valid");
        outcome = productEntityHome.getChangeRequestProduct().getDefaultName();
        Assert.assertEquals(outcome, "productRefTest1");

        Assert.assertEquals(productEntityHome.getSelectedCodeItems().size(), codeCount1 - 1);
        Assert.assertEquals(productEntityHome.getSelectedUnitItems().size(), unitCount1 - 1);
        Assert.assertEquals(productEntityHome.getAvailableCodeItems().size(), 1);
        Assert.assertEquals(productEntityHome.getAvailableUnitItems().size(), 1);

        hibernateSession.flush();
        hibernateSession.clear();
        productEntityHome.clearInstance();
        
        // delete entities
        product1 = (ChangeRequestProduct) hibernateSession.get(ChangeRequestProduct.class, product1.getId());
        hibernateSession.delete(product1);
        product2 = (ChangeRequestProduct) hibernateSession.get(ChangeRequestProduct.class, product2.getId());
        hibernateSession.delete(product2);
        product3 = (ChangeRequestProduct) hibernateSession.get(ChangeRequestProduct.class, product3.getId());
        hibernateSession.delete(product3);

        unit1 = (ChangeRequestUnit) hibernateSession.get(ChangeRequestUnit.class, unit1.getId());
        hibernateSession.delete(unit1);
        unit2 = (ChangeRequestUnit) hibernateSession.get(ChangeRequestUnit.class, unit2.getId());
        hibernateSession.delete(unit2);
        unit3 = (ChangeRequestUnit) hibernateSession.get(ChangeRequestUnit.class, unit3.getId());
        hibernateSession.delete(unit3);

        code1 = (ChangeRequestCode) hibernateSession.get(ChangeRequestCode.class, code1.getId());
        hibernateSession.delete(code1);
        code2 = (ChangeRequestCode) hibernateSession.get(ChangeRequestCode.class, code2.getId());
        hibernateSession.delete(code2);
        code3 = (ChangeRequestCode) hibernateSession.get(ChangeRequestCode.class, code3.getId());
        hibernateSession.delete(code3);
        
        hibernateSession.flush();
        hibernateSession.clear();

    }

    private ChangeRequestProduct createProduct(final String defaultName) {
        String outcome;
        productEntityHome.clearInstance();
        outcome = productEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid");
        final ChangeRequestProduct entity = productEntityHome.getInstance();
        Assert.assertNotNull(entity);
        entity.setDefaultName(defaultName);
        entity.setEnabled(true);
        outcome = productEntityHome.persist();
        Assert.assertEquals(outcome, "persisted");
        return entity;
    }

    private ChangeRequestUnit createUnit(final String defaultName) {
        String outcome;
        unitEntityHome.clearInstance();
        outcome = unitEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid");
        final ChangeRequestUnit entity = unitEntityHome.getInstance();
        Assert.assertNotNull(entity);
        entity.setDefaultName(defaultName);
        entity.setEnabled(true);
        outcome = unitEntityHome.persist();
        Assert.assertEquals(outcome, "persisted");
        return entity;
    }

    private ChangeRequestCode createCode(final String defaultName) {
        String outcome;
        codeEntityHome.clearInstance();
        outcome = codeEntityHome.setEntityId(null);
        Assert.assertEquals(outcome, "valid");
        final ChangeRequestCode entity = codeEntityHome.getInstance();
        Assert.assertNotNull(entity);
        entity.setDefaultName(defaultName);
        entity.setEnabled(true);
        outcome = codeEntityHome.persist();
        Assert.assertEquals(outcome, "persisted");
        return entity;
    }

}
