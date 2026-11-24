package br.com.estoque.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Numero quebrado em campo inteiro (ex.: quantidade 2.7) passa a ser recusado.
    // O padrao do Jackson e cortar a parte decimal e gravar 2 sem avisar ninguem.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer recusarDecimalEmCampoInteiro() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}
