package net.wohlfart.framework.entities;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.PrePersist;

import net.wohlfart.framework.IllegalParameterException;

import org.hibernate.classic.Lifecycle;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;

/**
 * custom hibernate listener to call onPreUpdate Methods on the entitys
 * this feature comes free in JPA, but need some setup for plain hibernate sessions
 * use:
 *   <event type="pre-update">
 *      <listener class="...PreUpdateListener"/>
 *   </event>
 * 
 * to have this class called on any database updates
 * 
 * @author Michael Wohlfart
 *
 */
public class PreUpdateListener implements Serializable, PreUpdateEventListener {
    
    public final static String METHOD_NAME = "onPreUpdate";
    
    @Override
    public boolean onPreUpdate(PreUpdateEvent event)  {
        Object entity = event.getEntity();
        if (entity instanceof PreUpdateEventListener) {
            return ((PreUpdateEventListener)entity).onPreUpdate(event);
        } else {
            return Lifecycle.NO_VETO;
        }
        
        /*
        Method[] methods = entity.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(METHOD_NAME)) {
                Object result;
                try {
                    result = method.invoke(entity, event);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    } else {
                        throw new IllegalArgumentException(METHOD_NAME + "in " + entity + " must return a boolean");
                    }
                } catch (IllegalArgumentException ex) {
                    // TODO Auto-generated catch block
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    // TODO Auto-generated catch block
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    // TODO Auto-generated catch block
                    ex.printStackTrace();
                }

            }
        }     
        return Lifecycle.NO_VETO;
        */
    }

}
