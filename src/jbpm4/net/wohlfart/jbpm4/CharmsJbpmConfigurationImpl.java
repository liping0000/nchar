package net.wohlfart.jbpm4;

import org.jbpm.api.ProcessEngine;
import org.jbpm.pvm.internal.cfg.ConfigurationImpl;

public class CharmsJbpmConfigurationImpl extends ConfigurationImpl {

    @Override
    public ProcessEngine buildProcessEngine() {
        return CharmsProcessEngine.create(this);
    }
}
