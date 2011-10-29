package net.wohlfart.framework.debug;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: add as hidden div with this values
// to the root template for debugging...

@Name("contextStatistics")
@Scope(ScopeType.EVENT)
// @BypassInterceptors
public class ContextStatistics {

    private final static Logger LOGGER = LoggerFactory.getLogger(ContextStatistics.class);

    @Create
    public void createComponent() {
        LOGGER.debug("createComponent called");
    }

    @Destroy
    public void destroyComponent() {
        LOGGER.debug("destroyComponent called");
    }

    public List<ContextComponent> getList() {

        final List<ContextComponent> list = new ArrayList<ContextComponent>();

        final Context methodContext = Contexts.getMethodContext();
        if (methodContext != null) {
            for (final String name : methodContext.getNames()) {
                list.add(new ContextComponent("method ", name, methodContext.get(name).getClass().getName()));
            }
        }

        final Context eventContext = Contexts.getEventContext();
        if (eventContext != null) {
            for (final String name : eventContext.getNames()) {
                list.add(new ContextComponent("event ", name, eventContext.get(name).getClass().getName()));
            }
        }

        final Context pageContext = Contexts.getPageContext();
        if (pageContext != null) {
            for (final String name : pageContext.getNames()) {
                list.add(new ContextComponent("page ", name, pageContext.get(name).getClass().getName()));
            }
        }

        final Context conversationContext = Contexts.getConversationContext();
        if (conversationContext != null) {
            for (final String name : conversationContext.getNames()) {
                list.add(new ContextComponent("conversation ", name, conversationContext.get(name).getClass().getName()));
            }
        }

        final Context sessionContext = Contexts.getSessionContext();
        if (sessionContext != null) {
            for (final String name : sessionContext.getNames()) {
                list.add(new ContextComponent("session ", name, sessionContext.get(name).getClass().getName()));
            }
        }

        final Context businessProcessContext = Contexts.getBusinessProcessContext();
        if (businessProcessContext != null) {
            for (final String name : businessProcessContext.getNames()) {
                list.add(new ContextComponent("businessProcess ", name, businessProcessContext.get(name).getClass().getName()));
            }
        }

        final Context applicationProcessContext = Contexts.getApplicationContext();
        if (applicationProcessContext != null) {
            for (final String name : applicationProcessContext.getNames()) {
                list.add(new ContextComponent("application ", name, applicationProcessContext.get(name).getClass().getName()));
            }
        }

        return list;
    }

    public static class ContextComponent {

        private final String contextName;
        private final String name;
        private final String clazz;

        ContextComponent(final String contextName, final String name, final String clazz) {
            this.contextName = contextName;
            this.name = name;
            this.clazz = clazz;
        }

        public String getContextName() {
            return contextName;
        }

        public String getName() {
            return name;
        }

        public String getClazz() {
            return clazz;
        }
    }
}

/*
 * example content:
 * 
 * 
 * 
 * facelets.ui.DebugOutput 72210 javax.faces.request.charset 8
 * org.ajax4jsf.application.AjaxStateHolder 95338
 * org.ajax4jsf.application.AjaxStateManager.view_sequence 77
 * org.jboss.seam.CONVERSATION#23$entityManager 704
 * org.jboss.seam.CONVERSATION#23$org.jboss.seam.bpm.businessProcess 114
 * org.jboss.seam.CONVERSATION#23$org.jboss.seam.core.conversation 226
 * org.jboss.seam.CONVERSATION#23$org.jboss.seam.international.statusMessages
 * 252 org.jboss.seam.CONVERSATION#23$org.jboss.seam.pageflow.pageflow 125
 * org.jboss.seam.CONVERSATION#23$org.jboss.seam.persistence.persistenceContexts
 * 301 org.jboss.seam.CONVERSATION#23$taskAction 1775 org.jboss.seam.bpm.actor
 * 276 org.jboss.seam.core.conversationEntries 758
 * org.jboss.seam.international.localeSelector 337
 * org.jboss.seam.international.timeZoneSelector 312
 * org.jboss.seam.security.credentials 5
 * org.jboss.seam.security.defaultResolverChain 9246
 * org.jboss.seam.security.identity 1517
 * org.jboss.seam.security.management.authenticatedUser 2305
 * org.jboss.seam.security.rememberMe 716
 * org.jboss.seam.security.ruleBasedPermissionResolver 170
 * org.jboss.seam.theme.themeSelector 252 org.jboss.seam.web.session 110 total
 * size of the current session: 187138
 * 
 * conversation taskInstanceItemList
 * net.wohlfart.jbpm.queries.TaskInstanceItemList_$$_javassist_seam_15
 * conversation org.jboss.seam.pageflow.pageflow
 * org.jboss.seam.pageflow.Pageflow conversation taskAction
 * net.wohlfart.jbpm.TaskAction_$$_javassist_seam_14 conversation
 * org.jboss.seam.persistence.persistenceContexts
 * org.jboss.seam.persistence.PersistenceContexts conversation
 * changeRequestFacetStateFactory net.wohlfart.jbpm.changerequest.fabs.
 * ChangeRequestFacetStateFactory_$$_javassist_seam_16 conversation
 * org.jboss.seam.core.conversation org.jboss.seam.core.Conversation
 * conversation changeRequestFacetState
 * net.wohlfart.charms.entity.ChangeRequestFacetState conversation
 * org.jboss.seam.international.statusMessages
 * org.jboss.seam.faces.FacesMessages conversation
 * org.jboss.seam.bpm.businessProcess org.jboss.seam.bpm.BusinessProcess
 * conversation entityManager
 * org.jboss.seam.persistence.ManagedPersistenceContext session
 * org.ajax4jsf.application.AjaxStateManager.view_sequence java.lang.Integer
 * session org.jboss.seam.international.timeZoneSelector
 * net.wohlfart.charms.timezone.CustomTimezoneSelector session
 * org.jboss.seam.security.ruleBasedPermissionResolver
 * org.jboss.seam.security.permission.RuleBasedPermissionResolver session
 * org.jboss.seam.security.rememberMe org.jboss.seam.security.RememberMe session
 * org.ajax4jsf.application.AjaxStateHolder
 * org.ajax4jsf.application.AjaxStateHolder session
 * org.jboss.seam.security.identity net.wohlfart.security.CharmsIdentity session
 * org.jboss.seam.core.conversationEntries
 * org.jboss.seam.core.ConversationEntries session
 * org.jboss.seam.security.credentials org.jboss.seam.security.Credentials
 * session org.jboss.seam.security.defaultResolverChain
 * org.jboss.seam.security.permission.ResolverChain session
 * org.jboss.seam.international.localeSelector
 * net.wohlfart.charms.i18n.CustomLocaleSelector session
 * org.jboss.seam.bpm.actor org.jboss.seam.bpm.Actor session
 * org.jboss.seam.security.management.authenticatedUser
 * net.wohlfart.charms.entity.CharmsUser session
 * org.jboss.seam.theme.themeSelector
 * net.wohlfart.charms.theme.CustomThemeSelector session
 * org.jboss.seam.web.session org.jboss.seam.web.Session session
 * facelets.ui.DebugOutput com.sun.facelets.tag.ui.UIDebug$2 session
 * javax.faces.request.charset java.lang.String businessProcess
 * changeRequestDataBean
 * net.wohlfart.charms.entity.ChangeRequestDataBean_$$_javassist_19
 * businessProcess initiateDate java.sql.Timestamp businessProcess
 * changeRequestReferenceBean
 * net.wohlfart.charms.entity.ChangeRequestReferenceBean_$$_javassist_0
 * businessProcess initiateActorId java.lang.String businessProcess
 * changeRequestMessageBean
 * net.wohlfart.charms.entity.ChangeRequestMessageBean_$$_javassist_109
 * businessProcess changeRequestFolder
 * net.wohlfart.charms.entity.ChangeRequestFolder_$$_javassist_79 appliaction
 * changeRequestDataBeanUserTable.component org.jboss.seam.Component appliaction
 * handleRequest.component org.jboss.seam.Component appliaction
 * org.jboss.seam.theme.themeFactory.component org.jboss.seam.Component
 * appliaction charmsRoleTable.component org.jboss.seam.Component appliaction
 * org.jboss.seam.international.statusMessages.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.document.documentStore.component org.jboss.seam.Component
 * appliaction org.jboss.seam.international.timeZones.component
 * org.jboss.seam.Component appliaction org.jboss.seam.debug.contexts.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.ruleBasedPermissionResolver.component
 * org.jboss.seam.Component appliaction changeRequestPdfDocument.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.el.referenceCache.component org.jboss.seam.Component
 * appliaction org.jboss.seam.async.dispatcher.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.persistentPermissionResolver.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.bpm.taskInstancePriorityList.component
 * org.jboss.seam.Component appliaction org.jboss.seam.web.identityFilter
 * org.jboss.seam.web.IdentityFilter appliaction
 * org.jboss.seam.graphicImage.image.component org.jboss.seam.Component
 * appliaction charmsEmailTemplateTable.component org.jboss.seam.Component
 * appliaction org.apache.catalina.WELCOME_FILES [Ljava.lang.String; appliaction
 * org.jboss.seam.bpm.taskInstanceListForType.component org.jboss.seam.Component
 * appliaction org.jboss.seam.international.localeConfig.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.servlet.characterEncodingFilter
 * org.jboss.seam.web.CharacterEncodingFilter appliaction
 * org.jboss.seam.security.configurationFactory.component
 * org.jboss.seam.Component appliaction charmsPermission.component
 * org.jboss.seam.Component appliaction javax.servlet.context.tempdir
 * java.io.File appliaction trimStringConverter.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.ajax4jsfFilter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.pageflow.pageflow.component
 * org.jboss.seam.Component appliaction charmsUser.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.entityPermissionChecker
 * org.jboss.seam.security.EntityPermissionChecker appliaction
 * org.jboss.seam.ui.resource.webResource org.jboss.seam.ui.resource.WebResource
 * appliaction org.jboss.seam.web.redirectFilter
 * org.jboss.seam.web.RedirectFilter appliaction
 * org.jboss.seam.web.userPrincipal.component org.jboss.seam.Component
 * appliaction org.apache.catalina.jsp_classpath java.lang.String appliaction
 * org.jboss.seam.security.management.userSearch.component
 * org.jboss.seam.Component appliaction completeRequest.component
 * org.jboss.seam.Component appliaction org.jboss.seam.captcha.captcha.component
 * org.jboss.seam.Component appliaction runningJobTable.component
 * org.jboss.seam.Component appliaction reviewRequest.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.ui.entityIdentifierStore.component org.jboss.seam.Component
 * appliaction permissionTargetCollection.component org.jboss.seam.Component
 * appliaction charmsUserSessionLogger
 * net.wohlfart.CharmsUserSessionLogger_$$_javassist_seam_0 appliaction
 * org.jboss.seam.faces.facesContext.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.persistentPermissionResolver
 * org.jboss.seam.security.permission.PersistentPermissionResolver appliaction
 * org.apache.jasper.runtime.JspApplicationContextImpl
 * org.apache.jasper.runtime.JspApplicationContextImpl appliaction
 * changeRequestDataBeanAdminTable.component org.jboss.seam.Component
 * appliaction org.jboss.seam.remoting.remoting.component
 * org.jboss.seam.Component appliaction changeRequestFolderFactory.component
 * org.jboss.seam.Component appliaction org.jboss.seam.core.resourceBundle
 * net.wohlfart.charms.i18n.GlobalResourceBundle appliaction
 * org.jboss.seam.ui.facelet.mockServletContext.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.ui.facelet.faceletCompiler.component org.jboss.seam.Component
 * appliaction documentResource.component org.jboss.seam.Component appliaction
 * messageBundlesList.component org.jboss.seam.Component appliaction
 * org.jboss.seam.web.hotDeployFilter org.jboss.seam.web.HotDeployFilter
 * appliaction org.jboss.seam.bpm.actor.component org.jboss.seam.Component
 * appliaction charmsDataStoreActionBean.component org.jboss.seam.Component
 * appliaction org.jboss.seam.web.isUserInRole.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.international.timeZone.component org.jboss.seam.Component
 * appliaction org.jboss.seam.core.interpolator.component
 * org.jboss.seam.Component appliaction charmsUserSessionLogger.component
 * org.jboss.seam.Component appliaction org.apache.catalina.resources
 * org.apache.naming.resources.ProxyDirContext appliaction
 * nodeDefinitionActionBean.component org.jboss.seam.Component appliaction
 * selectUserId.component org.jboss.seam.Component appliaction
 * org.jboss.seam.core.manager.component org.jboss.seam.Component appliaction
 * _init_parameter_org.richfaces.CONTROL_SKINNING_LEVEL java.lang.Object
 * appliaction timerActionBean.component org.jboss.seam.Component appliaction
 * org.jboss.seam.mail.mailSession org.jboss.seam.mail.MailSession appliaction
 * org.jboss.seam.core.ConversationIdGenerator
 * org.jboss.seam.core.ConversationIdGenerator_$$_javassist_seam_9 appliaction
 * taskInstanceActionBean.component org.jboss.seam.Component appliaction
 * org.jboss.seam.mail.mailSession.component org.jboss.seam.Component
 * appliaction org.jboss.seam.core.resourceLoader.component
 * org.jboss.seam.Component appliaction com.sun.faces.ApplicationAssociate
 * com.sun.faces.application.ApplicationAssociate appliaction
 * org.jboss.seam.core.locale.component org.jboss.seam.Component appliaction
 * createTimerActionList.component org.jboss.seam.Component appliaction
 * org.jboss.seam.faces.httpError.component org.jboss.seam.Component appliaction
 * emailDelegateList.component org.jboss.seam.Component appliaction
 * org.jboss.seam.security.identity.component org.jboss.seam.Component
 * appliaction org.jboss.seam.bpm.taskInstance.component
 * org.jboss.seam.Component appliaction
 * _init_parameter_org.richfaces.queue.global.enabled java.lang.String
 * appliaction org.jboss.seam.navigation.pages org.jboss.seam.navigation.Pages
 * appliaction org.jboss.seam.international.localeSelector.component
 * org.jboss.seam.Component appliaction jboss.kernel:service=Kernel
 * org.jboss.kernel.Kernel appliaction timeBean.component
 * org.jboss.seam.Component appliaction org.jboss.seam.el.referenceCache
 * org.jboss.seam.el.JBossELReferenceCache_$$_javassist_seam_1 appliaction
 * org.jboss.seam.security.rememberMe.component org.jboss.seam.Component
 * appliaction org.jboss.seam.web.loggingFilter org.jboss.seam.web.LoggingFilter
 * appliaction org.jboss.seam.remoting.remoting org.jboss.seam.remoting.Remoting
 * appliaction org.jboss.seam.security.identifierPolicy
 * org.jboss.seam.security.permission.IdentifierPolicy appliaction
 * org.jboss.seam.ui.graphicImage.graphicImageStore.component
 * org.jboss.seam.Component appliaction menuSetup.component
 * org.jboss.seam.Component appliaction org.ajax4jsf.webapp.WebXml
 * org.ajax4jsf.webapp.WebXml appliaction seam.context.classLoader
 * java.lang.ref.WeakReference appliaction actorTaskTable.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.ui.EntityConverter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.theme.themeSelector.component
 * org.jboss.seam.Component appliaction userPropertiesActionBean.component
 * org.jboss.seam.Component appliaction changeRequestCodeHome.component
 * org.jboss.seam.Component appliaction charmsEntityManagerFactory.component
 * org.jboss.seam.Component appliaction charmsEmailMessageAction.component
 * org.jboss.seam.Component appliaction changeRequestUnitHome.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.identifierPolicy.component org.jboss.seam.Component
 * appliaction org.jboss.seam.bpm.jbpmContext.component org.jboss.seam.Component
 * appliaction org.jboss.seam.web.ajax4jsfFilterInstantiator.component
 * org.jboss.seam.Component appliaction charmsRoleActionBean.component
 * org.jboss.seam.Component appliaction createTimerActionBean.component
 * org.jboss.seam.Component appliaction charmsPermissionActionBean.component
 * org.jboss.seam.Component appliaction entityManager.component
 * org.jboss.seam.Component appliaction org.apache.InstanceManager
 * org.jboss.web.tomcat.service.TomcatInjectionContainer appliaction
 * org.richfaces.util.RenderPhaseComponentVisitorUtils
 * [Lorg.richfaces.event.RenderPhaseComponentVisitor; appliaction
 * changeRequestCodeTable.component org.jboss.seam.Component appliaction
 * org.jboss.seam.web.exceptionFilter org.jboss.seam.web.ExceptionFilter
 * appliaction org.jboss.seam.core.resourceBundle.component
 * org.jboss.seam.Component appliaction org.jboss.seam.version java.lang.String
 * appliaction org.jboss.seam.exception.exceptions.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.bpm.taskInstanceList.component org.jboss.seam.Component
 * appliaction changeRequestDataBeanFactory.component org.jboss.seam.Component
 * appliaction org.jboss.seam.core.events.component org.jboss.seam.Component
 * appliaction net.wohlfart.sessionList java.util.ArrayList appliaction
 * org.jboss.seam.core.validators.component org.jboss.seam.Component appliaction
 * _init_parameter_org.richfaces.LoadScriptStrategy java.lang.String appliaction
 * org.jboss.seam.bpm.pooledTaskInstanceList.component org.jboss.seam.Component
 * appliaction processInstanceItemList.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.passwordHash.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.async.asynchronousExceptionHandler.component
 * org.jboss.seam.Component appliaction org.jboss.seam.web.ajax4jsfFilter
 * org.jboss.seam.web.Ajax4jsfFilter appliaction
 * org.jboss.seam.ui.facelet.faceletsJBossLogging.component
 * org.jboss.seam.Component appliaction org.jboss.seam.security.JpaIdentityStore
 * net.wohlfart.security.CharmsIdentityStore appliaction
 * org.jboss.seam.persistence.persistenceProvider.component
 * org.jboss.seam.Component appliaction org.jboss.seam.core.expressions
 * org.jboss.seam.faces.FacesExpressions appliaction
 * org.jboss.seam.core.conversationPropagation.component
 * org.jboss.seam.Component appliaction org.jboss.seam.web.parameters.component
 * org.jboss.seam.Component appliaction
 * _init_parameter_org.richfaces.LoadStyleStrategy java.lang.String appliaction
 * org.jboss.seam.captcha.captchaImage org.jboss.seam.captcha.CaptchaImage
 * appliaction org.jboss.seam.security.identityManager.component
 * org.jboss.seam.Component appliaction
 * changeRequestMessageBeanFactory.component org.jboss.seam.Component
 * appliaction org.jboss.seam.ui.resource.webResource.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.facesSecurityEvents.component
 * org.jboss.seam.Component appliaction org.jboss.seam.ui.entityLoader.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.international.messagesFactory.component
 * org.jboss.seam.Component appliaction org.jboss.seam.faces.redirect.component
 * org.jboss.seam.Component appliaction org.jboss.seam.bpm.transition.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.international.timeZoneSelector.component
 * org.jboss.seam.Component appliaction org.jboss.seam.web.session.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.transaction.facesTransactionEvents
 * org.jboss.seam.transaction.FacesTransactionEvents appliaction
 * org.jboss.seam.transaction.synchronizations.component
 * org.jboss.seam.Component appliaction org.jboss.seam.faces.facesPage.component
 * org.jboss.seam.Component appliaction accountSetup.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.identityFilter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.ui.graphicImage.graphicImageResource.component
 * org.jboss.seam.Component appliaction contextContents.component
 * org.jboss.seam.Component appliaction changeRequestFolderActionBean.component
 * org.jboss.seam.Component appliaction org.ajax4jsf.webapp.PollEventsManager
 * org.ajax4jsf.webapp.PollEventsManager appliaction
 * userSessionInitializer.component org.jboss.seam.Component appliaction
 * org.jboss.seam.bpm.processInstanceFinder.component org.jboss.seam.Component
 * appliaction com.sun.faces.config.WebConfiguration
 * com.sun.faces.config.WebConfiguration appliaction
 * org.jboss.seam.security.credentials.component org.jboss.seam.Component
 * appliaction supportedTimezones.component org.jboss.seam.Component appliaction
 * org.jboss.seam.ui.clientUidSelector.component org.jboss.seam.Component
 * appliaction org.jboss.seam.international.localeConfig
 * org.jboss.seam.international.LocaleConfig appliaction
 * org.jboss.seam.security.entityPermissionChecker.component
 * org.jboss.seam.Component appliaction charmsSessionLog.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.faces.dataModels.component org.jboss.seam.Component
 * appliaction org.jboss.seam.core.expressions.component
 * org.jboss.seam.Component appliaction chartGenerator.component
 * org.jboss.seam.Component appliaction mailSessionProvider
 * net.wohlfart.MailSessionProvider_$$_javassist_seam_2 appliaction
 * org.jboss.seam.transaction.facesTransactionEvents.component
 * org.jboss.seam.Component appliaction changerequestStart.component
 * org.jboss.seam.Component appliaction org.jboss.seam.core.init
 * org.jboss.seam.core.Init appliaction org.jboss.seam.core.contexts.component
 * org.jboss.seam.Component appliaction processRequest.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.remoting.gwt.gwtToSeamAdapter.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.framework.currentDate.component org.jboss.seam.Component
 * appliaction org.jboss.deployers.structure.spi.DeploymentUnit
 * org.jboss.deployers.vfs.plugins.structure.AbstractVFSDeploymentUnit
 * appliaction org.jboss.seam.servlet.characterEncodingFilter.component
 * org.jboss.seam.Component appliaction implementRequest.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.jpaPermissionStore.component org.jboss.seam.Component
 * appliaction org.jboss.seam.core.conversationEntries.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.debug.introspector.component org.jboss.seam.Component
 * appliaction org.jboss.seam.web.multipartFilter
 * org.jboss.seam.web.MultipartFilter appliaction
 * org.jboss.seam.navigation.pages.component org.jboss.seam.Component
 * appliaction taskInstanceItemList.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.configuration
 * org.jboss.seam.security.Configuration$1 appliaction
 * org.jboss.seam.faces.dateConverter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.bpm.pooledTask.component org.jboss.seam.Component
 * appliaction com.sun.faces.sunJsfJs [C appliaction
 * org.jboss.seam.core.conversationListFactory.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.loggingFilter.component org.jboss.seam.Component
 * appliaction bootSequence.component org.jboss.seam.Component appliaction
 * charmsUserActionBean.component org.jboss.seam.Component appliaction
 * org.jboss.seam.core.ConversationIdGenerator.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.management.userAction.component
 * org.jboss.seam.Component appliaction org.jboss.seam.faces.renderer.component
 * org.jboss.seam.Component appliaction taskAction.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.faces.uiComponent.component org.jboss.seam.Component
 * appliaction org.jboss.seam.framework.currentTime.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.permissionMapper.component org.jboss.seam.Component
 * appliaction charmsLogTable.component org.jboss.seam.Component appliaction
 * charmsPermissionTable.component org.jboss.seam.Component appliaction
 * assignRequest.component org.jboss.seam.Component appliaction
 * org.jboss.seam.excel.exporter.excelExporter.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.exceptionFilter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.faces.switcher.component org.jboss.seam.Component
 * appliaction mailSessionProvider.component org.jboss.seam.Component
 * appliaction jbpmLogTable.component org.jboss.seam.Component appliaction
 * messageBundleSetup.component org.jboss.seam.Component appliaction
 * org.jboss.seam.security.jpaPermissionStore
 * net.wohlfart.security.CharmsPermissionStore appliaction
 * org.jboss.seam.security.permissionManager.component org.jboss.seam.Component
 * appliaction org.jboss.seam.bpm.businessProcess.component
 * org.jboss.seam.Component appliaction changeRequestProductTable.component
 * org.jboss.seam.Component appliaction emailRequest.component
 * org.jboss.seam.Component appliaction changeRequestUnitTable.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.facesSecurityEvents
 * org.jboss.seam.security.FacesSecurityEvents appliaction
 * org.jboss.seam.ui.facelet.mockHttpSession.component org.jboss.seam.Component
 * appliaction org.jboss.seam.excel.excelFactory.component
 * org.jboss.seam.Component appliaction changeRequestReferenceData.component
 * org.jboss.seam.Component appliaction refDataSetup.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.core.conversationStackFactory.component
 * org.jboss.seam.Component appliaction charmsEmailMessageTable.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.bpm.processInstance.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.JpaIdentityStore.component
 * org.jboss.seam.Component appliaction jbpmSetup.component
 * org.jboss.seam.Component appliaction changeRequestFacetStateFactory.component
 * org.jboss.seam.Component appliaction changeRequestFolderList.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.servletContexts.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.permission.permissionSearch.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.persistence.persistenceContexts.component
 * org.jboss.seam.Component appliaction processDefinitionItemList.component
 * org.jboss.seam.Component appliaction timeBean net.wohlfart.TimeBean
 * appliaction org.jboss.seam.properties java.util.HashMap appliaction
 * org.jboss.seam.ui.graphicImage.graphicImageResource
 * org.jboss.seam.ui.graphicImage.GraphicImageResource appliaction
 * org.jboss.seam.framework.currentDatetime.component org.jboss.seam.Component
 * appliaction org.jboss.seam.navigation.safeActions.component
 * org.jboss.seam.Component appliaction org.jboss.seam.bpm.jbpm.component
 * org.jboss.seam.Component appliaction charmsEntityManagerFactory
 * org.jboss.seam.persistence.EntityManagerFactory appliaction
 * org.jboss.seam.security.management.roleSearch.component
 * org.jboss.seam.Component appliaction changeRequestProductHome.component
 * org.jboss.seam.Component appliaction org.apache.AnnotationProcessor
 * org.apache.catalina.core.StandardContext$DummyAnnotationProcessor appliaction
 * _init_parameter_org.richfaces.CONTROL_SKINNING_CLASSES java.lang.Object
 * appliaction org.jboss.seam.transaction.transaction.component
 * org.jboss.seam.Component appliaction charmsUserTable.component
 * org.jboss.seam.Component appliaction processInstanceActionBean.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.security.management.roleAction.component
 * org.jboss.seam.Component appliaction org.jboss.seam.core.init.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.web.redirectFilter.component org.jboss.seam.Component
 * appliaction org.jboss.seam.security.permissionMapper
 * org.jboss.seam.security.permission.PermissionMapper appliaction
 * org.jboss.seam.ui.facelet.facesContextFactory.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.debug.jsf.debugRedirect.component org.jboss.seam.Component
 * appliaction org.jboss.seam.web.multipartFilter.component
 * org.jboss.seam.Component appliaction charmsEmailTemplateAction.component
 * org.jboss.seam.Component appliaction org.jboss.seam.bpm.jbpm
 * org.jboss.seam.bpm.Jbpm appliaction charmsRole.component
 * org.jboss.seam.Component appliaction processDefinitionActionBean.component
 * org.jboss.seam.Component appliaction
 * _init_parameter_org.richfaces.CONTROL_SKINNING java.lang.Object appliaction
 * org.jboss.seam.web.hotDeployFilter.component org.jboss.seam.Component
 * appliaction com.sun.faces.ApplicationImpl
 * com.sun.faces.application.ApplicationImpl appliaction
 * org.jboss.seam.faces.validation.component org.jboss.seam.Component
 * appliaction org.jboss.seam.ui.facelet.faceletsJBossLogging
 * org.jboss.seam.ui.facelet.FaceletsJBossLogging appliaction
 * org.jboss.seam.core.conversation.component org.jboss.seam.Component
 * appliaction org.jboss.seam.drools.spreadsheetComponent.component
 * org.jboss.seam.Component appliaction
 * org.jboss.seam.captcha.captchaImage.component org.jboss.seam.Component
 * appliaction httpSessionStatistics.component org.jboss.seam.Component
 */
