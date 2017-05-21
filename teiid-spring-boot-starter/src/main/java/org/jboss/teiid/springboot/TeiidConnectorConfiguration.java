package org.jboss.teiid.springboot;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class TeiidConnectorConfiguration {

    private Set<String> translators = new HashSet<>();
    
    private Set<String> vdbs = new HashSet<>();

    public Set<String> getTranslators() {
        return translators;
    }

    public void setTranslators(Set<String> translators) {
        this.translators = translators;
    }

    public Set<String> getVdbs() {
        return vdbs;
    }

    public void setVdbs(Set<String> vdbs) {
        this.vdbs = vdbs;
    }    
}
