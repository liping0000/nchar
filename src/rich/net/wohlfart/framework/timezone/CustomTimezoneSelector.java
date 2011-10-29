package net.wohlfart.framework.timezone;

import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.faces.model.SelectItem;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.TimeZoneSelector;

@Scope(ScopeType.SESSION)
@Name("org.jboss.seam.international.timeZoneSelector")
@BypassInterceptors
// we don't get any injections if we use this
@Install(precedence = APPLICATION, classDependencies = "javax.faces.context.FacesContext")
public class CustomTimezoneSelector extends TimeZoneSelector {

    /*
     * private static List<String> supported = Arrays.asList(new String[] {
     * "CET", "MET", "EET", "GMT", "WET", "Zulu", "CTT", "AET" });
     */


    public List<String> getSupportedTimezoneIds() {
        final List<String> supported = getSupported();
        // clone it:
        final List<String> result = new ArrayList<String>();
        for (final String name : supported) {
            result.add(name);
        }
        return result;
    }

    public List<SelectItem> getSupportedTimezones() {
        final List<String> supported = getSupported();
        final List<SelectItem> selectItems = new ArrayList<SelectItem>();
        final String[] timezoneIds = TimeZone.getAvailableIDs();
        for (final String id : timezoneIds) {
            if (supported.contains(id)) {
                selectItems.add(new SelectItem(id, TimeZone.getTimeZone(id).getDisplayName()));
            }
        }
        return selectItems;
    }

    public void selectTimeZoneId(final String timezoneId) {
        if ((timezoneId != null) && (getSupportedTimezoneIds().contains(timezoneId))) {
            selectTimeZone(timezoneId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getSupported() {
        return (List<String>) Component.getInstance("supportedTimezones", ScopeType.APPLICATION);
    }

}
