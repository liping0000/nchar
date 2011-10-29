package net.wohlfart.framework.i18n;

/**
 * this interface is used for all business objects that are being translated
 * 
 * 
 * @author Michael Wohlfart
 * 
 */
public interface ITranslateable {

    Long getId();

    // FIXME: why are here no setters?
    String getDefaultName();

    void setDefaultName(String defaultName);

    String getMessageCode();

    void setupMessageCode();
}
