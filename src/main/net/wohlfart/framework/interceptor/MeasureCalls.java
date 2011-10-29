package net.wohlfart.framework.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.seam.annotations.intercept.Interceptors;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Interceptors(TimingInterceptor.class)
public @interface MeasureCalls {
}
