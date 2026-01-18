package io.hqwu.commons.validator;

import io.hqwu.commons.annotation.constraints.support.EmailsValidator;
import io.hqwu.commons.annotation.constraints.support.NumbersInMessageInterpolator;
import io.hqwu.commons.annotation.constraints.support.PatternValidatorForBlank;
import io.hqwu.commons.annotation.constraints.support.SizeValidatorForBlankCharSequence;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
        setMessageInterpolator(new NumbersInMessageInterpolator(
                (AbstractMessageInterpolator) Validation.byDefaultProvider().configure().getDefaultMessageInterpolator()));
    }

    @Override
    protected void postProcessConfiguration(jakarta.validation.Configuration<?> configuration) {
        HibernateValidatorConfiguration config = Validation.byProvider(HibernateValidator.class).configure();
        customizeConstraintMappings(config);
    }

    private void customizeConstraintMappings(HibernateValidatorConfiguration config) {
        config.addMapping(createConstraintMapping(config, Size.class, SizeValidatorForBlankCharSequence.class));
        config.addMapping(createConstraintMapping(config, Pattern.class, PatternValidatorForBlank.class));
        config.addMapping(createConstraintMapping(config, Email.class, EmailsValidator.class));
    }

    private <A extends Annotation> ConstraintMapping createConstraintMapping(
            HibernateValidatorConfiguration config,
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
