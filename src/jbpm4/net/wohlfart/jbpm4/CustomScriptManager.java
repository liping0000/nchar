package net.wohlfart.jbpm4;

import groovy.lang.GroovyClassLoader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jbpm.api.JbpmException;
import org.jbpm.pvm.internal.script.EnvironmentBindings;
import org.jbpm.pvm.internal.script.GroovyScriptEngineFactory;
import org.jbpm.pvm.internal.script.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this is a customized script manager, we use groovy scripts only
 * they are used at all places through the process definition
 * 
 * @author Michael Wohlfart
 */
public class CustomScriptManager extends ScriptManager {


    private final static Logger LOGGER = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * the languages can be set up in the config:
     * 
     * <script-language name="juel" factory="org.jbpm.pvm.internal.script.JuelScriptEngineFactory" />
     * <script-language name="bsh" factory="org.jbpm.pvm.internal.script.BshScriptEngineFactory" />
     * <script-language name="groovy" factory="org.jbpm.pvm.internal.script.GroovyScriptEngineFactory" />
     */
    public CustomScriptManager() {
        scriptEngineManager = new ScriptEngineManager();

        // we don't use anything else but the groovy language yet
        //
        // scriptEngineManager.registerEngineName("juel", new org.jbpm.pvm.internal.script.JuelScriptEngineFactory());
        // scriptEngineManager.registerEngineName("bsh", new org.jbpm.pvm.internal.script.BshScriptEngineFactory());
        final GroovyScriptEngineFactory groovyEngineFab = new org.jbpm.pvm.internal.script.GroovyScriptEngineFactory();
        scriptEngineManager.registerEngineName(groovyEngineFab.getEngineName(), groovyEngineFab);
    }

    @SuppressWarnings("rawtypes")
    // the caller is responsible for setting the task environment
    @Override
    protected Object evaluate(final ScriptEngine scriptEngine, final String script) {

        LOGGER.debug("evaluate called for {}", script);

        // Bindings bindings = new CustomEnvironmentBindings();
        // we can add any custom binding here if we override the EnvironmentBindings
        final Bindings bindings = new EnvironmentBindings(readContextNames, writeContextName);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        if (!bindings.containsKey("task")) {
            LOGGER.warn("calling a script and no task in context, the script is [{}]", script);
        } else {
            LOGGER.info("got a task in the context >{}<", bindings.get("task"));
        }

        try {
            // we get a nullpointer exception if we have a comments only script, try to prevent this
            // by checking for a nullpointer clazz before calling eval on tne script engine:
            Class clazz = new GroovyClassLoader().parseClass(script);
            if (clazz == null) {
                LOGGER.debug("script parsed to null");
                return null;
            } else {
                final Object result = scriptEngine.eval(script);
                LOGGER.debug("script evaluated to {}", result);
                return result;
            }
        } catch (final Exception ex) {
            LOGGER.warn("script error in script, the error is: {}, the script is: [{}], throwing error ", ex.getMessage(), script);
            throw new JbpmException("script evaluation error: " + ex.getMessage(), ex);
        } 
    }
}
