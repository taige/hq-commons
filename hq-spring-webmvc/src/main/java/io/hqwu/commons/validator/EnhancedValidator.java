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
 * 增强型验证器，扩展了 Spring 的 {@link LocalValidatorFactoryBean}。
 * <p>
 * 该验证器通过自定义配置增强了标准 Bean Validation 的功能，主要包括：
 * <ul>
 *   <li>使用 {@link NumbersInMessageInterpolator} 处理 {@link io.hqwu.commons.annotation.constraints.NumbersIn}
 *       注解的消息插值，支持数组参数的格式化输出</li>
 *   <li>为 {@link Size}、{@link Pattern}、{@link Email} 等标准约束注解注册自定义校验器，
 *       增强对空白字符串的处理能力</li>
 * </ul>
 * </p>
 * <p>
 * 自定义约束映射：
 * <ul>
 *   <li>{@link Size} → {@link SizeValidatorForBlankCharSequence}：允许空白字符序列通过尺寸校验</li>
 *   <li>{@link Pattern} → {@link PatternValidatorForBlank}：允许空白字符串通过正则表达式校验</li>
 *   <li>{@link Email} → {@link EmailsValidator}：支持校验逗号分隔的多个邮箱地址</li>
 * </ul>
 * </p>
 * <p>
 * 该类通常作为 Spring Bean 注册到容器中，替代默认的 {@link LocalValidatorFactoryBean}，
 * 为应用提供更灵活的验证能力。
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see LocalValidatorFactoryBean
 * @see NumbersInMessageInterpolator
 * @see SizeValidatorForBlankCharSequence
 * @see PatternValidatorForBlank
 * @see EmailsValidator
 * @see HibernateValidator
 * @see ConstraintMapping
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
        super.postProcessConfiguration(configuration);
        // Apply customizations to the configuration provided by Spring
        if (configuration instanceof HibernateValidatorConfiguration) {
            customizeConstraintMappings((HibernateValidatorConfiguration) configuration);
        }
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
