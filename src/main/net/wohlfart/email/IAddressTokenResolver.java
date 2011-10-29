package net.wohlfart.email;

import java.util.Set;

import net.wohlfart.authentication.entities.CharmsUser;

import org.hibernate.Session;
import org.jbpm.pvm.internal.model.ExecutionImpl;

public interface IAddressTokenResolver {

    Set<CharmsUser> resolve(String expression, ExecutionImpl executionImpl, Session session);

}
