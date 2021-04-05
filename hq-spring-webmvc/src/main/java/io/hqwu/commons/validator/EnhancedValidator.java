package io.hqwu.commons.validator;

import io.hqwu.commons.annotation.constraints.support.EmailsValidator;
import io.hqwu.commons.annotation.constraints.support.NumbersInMessageInterpolator;
import io.hqwu.commons.annotation.constraints.support.PatternValidatorForBlank;
import io.hqwu.commons.annotation.constraints.support.SizeValidatorForBlankCharSequence;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.annotation.Annotation;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-04-04
 * Time: 4:06 p.m.
 */
public class EnhancedValidator extends LocalValidatorFactoryBean {

    public EnhancedValidator() {
        setMessageInterpolator(new NumbersInMessageInterpolator());
    }

    @Override
    protected void postProcessConfiguration(javax.validation.Configuration<?> configuration) {
        if(configuration instanceof ConfigurationImpl) {
            customizeConstraintMappings((ConfigurationImpl) configuration);
        }
    }

    private void customizeConstraintMappings(ConfigurationImpl config) {
        config.addMapping(createConstraintMapping(config, Size.class, SizeValidatorForBlankCharSequence.class));
        config.addMapping(createConstraintMapping(config, Pattern.class, PatternValidatorForBlank.class));
        config.addMapping(createConstraintMapping(config, Email.class, EmailsValidator.class));
    }

    private <A extends Annotation> ConstraintMapping createConstraintMapping(
            ConfigurationImpl config,
            Class<A> annotationClass,
            Class<? extends ConstraintValidator<A, ?>> validator) {
        ConstraintMapping constraintMapping = config.createConstraintMapping();
        constraintMapping
                .constraintDefinition(annotationClass)
                .includeExistingValidators(false)
                .validatedBy(validator);
        return constraintMapping;
    }

}
