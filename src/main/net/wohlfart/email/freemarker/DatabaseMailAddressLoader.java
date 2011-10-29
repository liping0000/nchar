package net.wohlfart.email.freemarker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.email.IAddressTokenResolver;
import net.wohlfart.email.entities.CharmsEmailTemplate;
import net.wohlfart.email.entities.CharmsEmailTemplateReceiver;

import org.hibernate.Session;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class loads the email receivers or senders, this solves the problem of
 * finding the proper mail addresses from the process instance context
 * 
 * @author Michael Wohlfart
 */
public class DatabaseMailAddressLoader {

    private final static Logger               LOGGER           = LoggerFactory.getLogger(DatabaseMailAddressLoader.class);

    // a resolver chain for email/tokens in the receiver or sender field of a
    // template
    private final List<IAddressTokenResolver> addressResolvers = new ArrayList<IAddressTokenResolver>();

    public void add(final IAddressTokenResolver addressTokenResolver) {
        addressResolvers.add(addressTokenResolver);
    }

    /**
     * return the receiver and its locale from the template definition
     * 
     * the receiver needs to be resolved in the context of the execution
     * instance
     */
    @SuppressWarnings("unchecked")
    @Transactional(TransactionPropagationType.MANDATORY)
    public List<CharmsUser> getReceivers(final CharmsEmailTemplate template, final ExecutionImpl execution, final Session session) {

        // first we need the address expressions for the template
        final List<String> expressions = session.getNamedQuery(CharmsEmailTemplateReceiver.FIND_EXPRESSION_BY_TEMPLATE_ID).setParameter("id", template.getId())
                .list();
        LOGGER.debug("found list of expressions for receiver: {}", expressions);

        // second we need to resolve this expressions within the current
        // executionContext
        final ArrayList<CharmsUser> receivers = new ArrayList<CharmsUser>();
        for (final String expression : expressions) {
            if ((expression != null) && (expression.trim().length() > 0)) {
                receivers.addAll(resolveExpression(expression, execution, session));
            }
        }
        return receivers;
    }

    @Transactional(TransactionPropagationType.MANDATORY)
    public CharmsUser getSender(final CharmsEmailTemplate template, final ExecutionImpl execution, final Session session) {

        // first we need the address expressions for the template
        final String expression = (String) session.getNamedQuery(CharmsEmailTemplate.FIND_SENDER_EXPRESSION_BY_ID).setParameter("id", template.getId())
                .uniqueResult();
        LOGGER.debug("found list of expressions for sender: {}", expression);

        // second we need to resolve this expressions within the current
        // executionContext
        final Set<CharmsUser> set = resolveExpression(expression, execution, session);
        if (set.size() == 1) {
            return set.toArray(new CharmsUser[1])[0];
        }

        LOGGER.warn("can't resolve sender for expression: " + expression + " creating user with default address " + " the template id is: " + template.getId()
                + " the event is: " + execution.getEvent());
        final CharmsUser user = new CharmsUser();

        // FIXME: there seems to be a problem here

        /*
         * 
         * 
         * 11:49:25,991 [pool-2-thread-3] WARN
         * net.wohlfart.email.freemarker.DatabaseMailAddressLoader - can't
         * resolve sender for expression: assignActor creating user with default
         * address the template id is: 12 the event is: event(taskRemind)
         * 11:49:25,991 [pool-2-thread-3] ERROR
         * org.jbpm.pvm.internal.cmd.ExecuteJobCmd - exception while executing
         * 'message[304]' java.lang.NullPointerException at
         * net.wohlfart.email.freemarker
         * .DatabaseMailAddressLoader.getSender(DatabaseMailAddressLoader
         * .java:102) at
         * net.wohlfart.email.freemarker.MailConfiguration.getSender
         * (MailConfiguration.java:123) at
         * net.wohlfart.jbpm4.mail.AbstractMailProducer
         * .produce(AbstractMailProducer.java:83) at
         * net.wohlfart.jbpm4.mail.CustomMailProducer
         * .doProduce(CustomMailProducer.java:147) at
         * net.wohlfart.jbpm4.mail.CustomMailProducer
         * .produce(CustomMailProducer.java:71) at
         * org.jbpm.jpdl.internal.activity
         * .MailActivity.perform(MailActivity.java:43) at
         * org.jbpm.jpdl.internal.
         * activity.JpdlAutomaticActivity.notify(JpdlAutomaticActivity.java:20)
         * at org.jbpm.pvm.internal.model.op.ExecuteEventListener.perform(
         * ExecuteEventListener.java:81) at
         * org.jbpm.pvm.internal.model.ExecutionImpl
         * .performAtomicOperationSync(ExecutionImpl.java:655) at
         * org.jbpm.pvm.internal
         * .model.op.ExecuteEventListenerMessage.execute(ExecuteEventListenerMessage
         * .java:154) at
         * org.jbpm.pvm.internal.cmd.ExecuteJobCmd.execute(ExecuteJobCmd
         * .java:76) at
         * org.jbpm.pvm.internal.cmd.ExecuteJobCmd.execute(ExecuteJobCmd
         * .java:42) at org.jbpm.pvm.internal.svc.DefaultCommandService.execute(
         * DefaultCommandService.java:42) at
         * org.jbpm.pvm.internal.tx.StandardTransactionInterceptor
         * .execute(StandardTransactionInterceptor.java:54) at
         * org.jbpm.pvm.internal
         * .svc.EnvironmentInterceptor.executeInNewEnvironment
         * (EnvironmentInterceptor.java:53) at
         * org.jbpm.pvm.internal.svc.EnvironmentInterceptor
         * .execute(EnvironmentInterceptor.java:40) at
         * org.jbpm.pvm.internal.svc.
         * RetryInterceptor.execute(RetryInterceptor.java:55) at
         * org.jbpm.pvm.internal
         * .svc.SkipInterceptor.execute(SkipInterceptor.java:43) at
         * org.jbpm.pvm.internal.jobexecutor.JobParcel.run(JobParcel.java:48) at
         * java
         * .util.concurrent.Executors$RunnableAdapter.call(Executors.java:441)
         * at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:303)
         * at java.util.concurrent.FutureTask.run(FutureTask.java:138) at
         * java.util
         * .concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor
         * .java:886) at
         * java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor
         * .java:908) at java.lang.Thread.run(Thread.java:619)
         */
        final String email = "charms@persman.de";
        final String name = "charms";

        // try {
        // email =
        // ApplicationPropertiesHashMap.getCurrent()
        // .getString(AbstractPropertiesHashMap.APPLICATION_EMAIL_ADDRESS,
        // email);// FIXME: parameterize
        //
        // name =
        // ApplicationPropertiesHashMap.getCurrent()
        // .getString(AbstractPropertiesHashMap.APPLICATION_NAME, name);//
        // FIXME: parameterize
        // } catch (java.lang.NullPointerException ex) {
        // ex.printStackTrace();
        // LOGGER.error("null pointer for ApplicationPropertiesHashMap.getCurrent(): {}",
        // ApplicationPropertiesHashMap.getCurrent());
        // }

        user.setEmail(email);
        user.setFirstname(name);
        return user;
    }

    /**
     * centralized method to resolve address strings, we use a resolver chain to
     * try resolving the address token, the caller expects a non null return
     * value
     * 
     * @param Expression
     * @param execution
     * @return
     */
    private Set<CharmsUser> resolveExpression(final String expression, final ExecutionImpl execution, final Session session) {

        if (expression == null) {
            LOGGER.warn("no expression found in email template");
            return new HashSet<CharmsUser>();
        }

        LOGGER.debug("starting resolver chain, session is: " + session);
        // try the resolver one by one until we hit the first match

        // it is very important to flush here since the resolvers access the
        // database
        // and need current information to be able to find the addresses for the
        // tasks
        session.flush();
        for (final IAddressTokenResolver resolver : addressResolvers) {
            final Set<CharmsUser> result = resolver.resolve(expression, execution, session);
            if (result != null) {
                return result;
            }
        }
        LOGGER.warn("no resolver found an address for the expression: {}, returning empty address set", expression);
        return new HashSet<CharmsUser>();
    }
}
