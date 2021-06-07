package io.hqwu.commons.validator;

import io.hqwu.commons.annotation.constraints.support.EmailsValidator;
import io.hqwu.commons.annotation.constraints.support.NumbersInMessageInterpolator;
import io.hqwu.commons.annotation.constraints.support.PatternValidatorForBlank;
import io.hqwu.commons.annotation.constraints.support.SizeValidatorForBlankCharSequence;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintValidator;
import javax.validation.Validation;
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
        setMessageInterpolator(new NumbersInMessageInterpolator(
                (AbstractMessageInterpolator) Validation.byDefaultProvider().configure().getDefaultMessageInterpolator()));
    }

    @Override
    protected void postProcessConfiguration(javax.validation.Configuration<?> configuration) {
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
