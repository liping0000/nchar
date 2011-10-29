package net.wohlfart.authentication.entities;

/**
 * Interface for holding actorIds, used for group and user objects this
 * interface is needed by the CharmsActorIdGenerator class which generates
 * unique actor ids based on the objects primary key
 * 
 * @author Michael Wohlfart
 */
public interface IActorIdHolder {

    /**
     * @return a prefix to distinguish groups and users
     */
    String getActorIdPrefix();

    /**
     * @return the actorID which contains the prefix, this value is immutable
     */
    String getActorId();

    /** 
     * @param actorId this is used by the actorIdHolder
     */
    void setActorId(String actorId);

    /**
     * @return this is the login name for users and the group name, both are mutable
     */
    String getName();

    /**
     * @return a label used in pulldowns and select poxes
     */
    String getLabel();

}
