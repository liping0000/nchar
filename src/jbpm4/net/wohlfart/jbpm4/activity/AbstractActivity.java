package net.wohlfart.jbpm4.activity;

import java.util.Map;

import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.activity.ExternalActivityBehaviour;
import org.jbpm.jpdl.internal.activity.ScriptActivity;
import org.jbpm.pvm.internal.model.ActivityImpl;

/**
 * base class for activities containing scripts
 * 
 */
public abstract class AbstractActivity extends ActivityImpl implements ExternalActivityBehaviour {

    // private final static Logger LOGGER =
    // LoggerFactory.getLogger(AbstractActivity.class);

    // XML tags needed for xml parsing during the binding phase

    // a generic task with this name will be generated and persisted:
    public static final String ATTRIBUTE_TASK_NAME           = "name";
    // a script that is executed
    public static final String TAG_SCRIPT_NAME               = "script";
    public static final String ATTRIBUTE_SCRIPTLANGUAGE_NAME = "language";
    // script text inside the script tag
    public static final String TAG_SCRIPT_TEXT               = "text";

    /* note: taskname is stored in super */

    /** content of the script */
    // protected String script;

    /** langiage of the script */
    // protected String language;

    // used in the execute method of the activity
    protected ScriptActivity   scriptActivity                = new ScriptActivity();

    @Override
    public abstract void signal(ActivityExecution execution, String signalName, Map<String, ?> parameters) throws Exception;

    @Override
    public abstract void execute(ActivityExecution execution) throws Exception;

    /**
     * perform the script for this activity, usualy called from within the
     * execute method of the activity when the task is created and
     * initialization work is performed
     * 
     * @return
     * 
     *         protected Object runScript() { if (StringUtils.isEmpty(script)) {
     *         LOGGER.info("no script to run"); return null; } else {
     *         LOGGER.info("running script..."); LOGGER.info(script);
     *         ScriptManager scriptManager =
     *         EnvironmentImpl.getFromCurrent(ScriptManager.class); Object
     *         returnValue = scriptManager.evaluateExpression(script, language);
     *         LOGGER.info("...finished script, return value is {} " ,
     *         returnValue); return returnValue; } }
     */

    /**
     * name of the task
     */
    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void setName(final String name) {
        super.setName(name);
    }

    /**
     * the script code
     */
    // public String getScript() {
    // return script;
    // }
    public void setScript(final String script) {
        scriptActivity.setScript(script);
        // this.script = script;
    }

    /**
     * the language for the script
     */
    // public String getScriptLanguage() {
    // return language;
    // }
    public void setScriptLanguage(final String language) {
        scriptActivity.setLanguage(language);
    }
}
