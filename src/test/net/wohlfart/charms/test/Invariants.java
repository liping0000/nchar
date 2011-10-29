package net.wohlfart.charms.test;

import net.wohlfart.authentication.entities.CharmsActorIdGenerator;
import net.wohlfart.authentication.entities.CharmsRole;
import net.wohlfart.authentication.entities.CharmsUser;
import net.wohlfart.authorization.entities.CharmsPermission;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Invariants  {


    @Test
    public void testActorIdColumnSize() {
        // calculate the max size for the actorId column,
        // unfortunately hibernate annotations need constants 
        // so we can't calculate the column size automatically at compile time....

        Assert.assertTrue(
                CharmsActorIdGenerator.ACTOR_ID_SIZE >=                         // the size we have
                    (CharmsActorIdGenerator.DECIMAL_FORMAT_TEMPLATE.length()                         
                            + java.lang.Math.max(                               // the size we need
                                    CharmsUser.ACTOR_PREFIX.length(), 
                                    CharmsRole.GROUP_ACTOR_PREFIX.length())

                    ),
                    "the column size for the actor id is too small in the user and role table"
        );
    }

    @Test
    public void testMySQLIndexSize() {
        // this unique contraint creates an index in MySQL which is limited to 1000Bytes (333 utf-8 chars)
        //   @Table(name = "CHARMS_PERMISSION", 
        //          uniqueConstraints = { @UniqueConstraint(columnNames = { "RECIPIENT_", "DISCRIMINATOR_", "TARGET_", "TARGET_ID_" }) })

        // lets limit it to 300
        Assert.assertTrue(
                300 >=                                                         // the size we have
                    CharmsPermission.MAX_RECIPIENT_LENGTH + 
                    CharmsPermission.MAX_DISCRIMINATOR_LENGTH +                // the size we need
                    CharmsPermission.MAX_TARGET_LENGTH +
                    30 // usually a (19,0) integer, so 30 should be ok
        );

        // also MAX_RECIPIENT_LENGTH of the permission table must be able to contain the names of the user and roles
        // that might get the permissions
        Assert.assertTrue(
                CharmsPermission.MAX_RECIPIENT_LENGTH >=
                    Math.max(CharmsRole.MAX_NAME_LENGTH, CharmsUser.MAX_LOGIN_LENGTH),
                    "the column size for the recipient is too small in the permission table"                    
        );  
    }
}
