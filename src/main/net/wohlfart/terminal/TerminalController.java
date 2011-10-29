package net.wohlfart.terminal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import net.wohlfart.terminal.commands.PerformCheckInvariants;
import net.wohlfart.terminal.commands.PerformCreateRoles;
import net.wohlfart.terminal.commands.PerformCreateUsers;
import net.wohlfart.terminal.commands.PerformFixChangeRequestDataPes;
import net.wohlfart.terminal.commands.PerformFixChangeRequestDataUserIds;
import net.wohlfart.terminal.commands.PerformFixDocsAndBlobs;
import net.wohlfart.terminal.commands.PerformFixExecutionVariables;
import net.wohlfart.terminal.commands.PerformFixJbpmBlobs;
import net.wohlfart.terminal.commands.PerformFixMessageUserIds;
import net.wohlfart.terminal.commands.PerformFixProcessId;
import net.wohlfart.terminal.commands.PerformFixUmlauts;
import net.wohlfart.terminal.commands.PerformLuceneIndex;
import net.wohlfart.terminal.commands.PerformLuceneOptimize;
import net.wohlfart.terminal.commands.PerformLuceneSearch;
import net.wohlfart.terminal.commands.PerformResetPasswords;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

// @Scope(CONVERSATION)
// @Name("terminalController")
// @AutoCreate
// @Controller(name = "terminalController", scope = Scope.REQUEST)
@Scope(ScopeType.CONVERSATION)
@Name("terminalController")
public class TerminalController implements Serializable {


    private final static Logger             LOGGER      = LoggerFactory.getLogger(TerminalController.class);

    private final ArrayList<IRemoteCommand> commands    = new ArrayList<IRemoteCommand>();

    private String                          commandList = "";

    @Create
    public void create() {
        commands.add(new PerformFixUmlauts());
        commands.add(new PerformFixProcessId());
        commands.add(new PerformFixMessageUserIds());
        commands.add(new PerformFixDocsAndBlobs());
        commands.add(new PerformFixChangeRequestDataUserIds());
        commands.add(new PerformFixChangeRequestDataPes());
        commands.add(new PerformFixJbpmBlobs());
        commands.add(new PerformLuceneIndex());
        commands.add(new PerformCheckInvariants());
        commands.add(new PerformLuceneSearch());
        commands.add(new PerformLuceneOptimize());
        commands.add(new PerformResetPasswords());
        commands.add(new PerformCreateUsers());
        commands.add(new PerformCreateRoles());
        commands.add(new PerformFixExecutionVariables());
        commandList = "";
        for (final IRemoteCommand cmd : commands) {
            commandList += cmd.getUsage();
            commandList += "<br />";
        }
    }

    // FIXME:remove out custom version of the terminal and use the primefaces
    // one...
    // the primefaces way
    @Transactional
    public String handleCommand(final String command, final String[] arguments) {
        final String argumentText = StringUtils.collectionToDelimitedString(Arrays.asList(arguments), " ");
        return handleCommand(command + " " + argumentText);
    }

    @WebRemote
    public String handleCommand(final String commandLine) {
        LOGGER.debug("handle command invoked for: >{}<", commandLine);

        try {
            for (final IRemoteCommand cmd : commands) {
                if (cmd.canHandle(commandLine)) {
                    return cmd.doHandle(commandLine);
                }
            }

            return "no matching command found for: " + commandLine + "<br />" + "--- list of available commands --- <br />" + commandList + "--- ";

        } catch (final Exception ex) {
            ex.printStackTrace();
            return ex.toString();
        }
    }

}
