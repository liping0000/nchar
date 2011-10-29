package net.wohlfart.framework.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.seam.annotations.intercept.Interceptors;

// see:
// http://openbook.galileocomputing.de/javainsel8/javainsel_24_006.htm#mj9408b58afe9999155a85d9fe2f0a481c
// seam interceptors only work on class level, see:
// http://lists.jboss.org/pipermail/seam-issues/2007-November/008259.html
// @Target({ElementType.TYPE, ElementType.METHOD})

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Interceptors(TransitionExceptionInterceptor.class)
public @interface TransitionStrategy {
}
