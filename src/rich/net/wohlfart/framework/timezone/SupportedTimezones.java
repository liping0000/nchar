package net.wohlfart.framework.timezone;

import java.util.AbstractList;
import java.util.List;

/**
 * This class is for customizing the selectable timezones in seam. It is
 * installed in application scope in components.xml
 * 
 * @author Michael Wohlfart
 */
public class SupportedTimezones extends AbstractList<String> {

    private List<String> supported;

    public void setTimezones(final List<String> supported) {
        this.supported = supported;
    }

    @Override
    public String get(final int index) {
        return supported.get(index);
    }

    @Override
    public int size() {
        return supported.size();
    }

}
