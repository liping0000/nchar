package net.wohlfart.terminal;

public interface IRemoteCommand {

    boolean canHandle(String commandLine);

    String doHandle(String commandLine);

    String getUsage();

}
