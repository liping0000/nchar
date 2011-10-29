package net.wohlfart.selenium;

import com.thoughtworks.selenium.*;
import org.testng.annotations.*;
import static org.testng.Assert.*;
import java.util.regex.Pattern;


public class Simple1  extends SeleneseTestNgHelper {
    
    @Test 
    public void testUntitled() throws Exception {
        selenium.open("/charms/pages/login.html?cid=1");
        selenium.type("f:username", "devel");
        selenium.type("f:password", "devel");
        selenium.click("f:login");
        selenium.waitForPageToLoad("30000");
        selenium.click("j_id151");
        selenium.waitForPageToLoad("30000");
        selenium.click("link=Änderungsanträge");
        selenium.click("link=Neuer Antrag");
        selenium.waitForPageToLoad("30000");
        selenium.type("changeRequest:itemIdNumberProperty:i", "test");
        selenium.select("changeRequest:productRef:productRefSelect", "label=Beschriftungs-Einheit");
        selenium.type("changeRequest:titleProperty:i", "test");
        selenium.click("changeRequest:submit");
        assertTrue(selenium.getConfirmation().matches("^Sind Sie Sicher[\\s\\S]$"));
    }
    
}


