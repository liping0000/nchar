package net.wohlfart.tools;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/*
 * this class will be used to export the database schema
 * 
 * 
 * @author Michael Wohlfart
 */
public class CharmsSchemaExport extends SchemaExport {

    public CharmsSchemaExport(final Configuration cfg) throws HibernateException {
        super(cfg);
    }

}
