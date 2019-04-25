package org.sturrock;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.logging.Logger;


public class LoggerProducer {  
    
    @Produces  
    public static Logger getLogger(InjectionPoint injectionPoint) {  
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getSimpleName());  
    }  
      
}  