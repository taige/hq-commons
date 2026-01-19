package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.NumbersIn;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.messageinterpolation.HibernateMessageInterpolatorContext;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@link NumbersIn} 注解的消息插值器实现,用于处理验证消息中数组参数的格式化输出。
 * <p>
 * 该插值器继承自 {@link AbstractMessageInterpolator},专门用于将 {@link NumbersIn}
 * 注解的 value 属性(数组类型)格式化为可读的字符串形式,以便在验证失败消息中正确显示。
 * 支持将 long[] 数组和 Object[] 数组转换为字符串表示形式。
 * </p>
 * <p>
 * 工作流程:
 * <ul>
 *   <li>检测当前验证约束是否为 {@link NumbersIn} 类型</li>
 *   <li>从 {@link HibernateMessageInterpolatorContext} 或约束描述符中提取参数变量</li>
 *   <li>若参数为数组类型,则使用 {@link Arrays#toString(long[])} 或 {@link Arrays#toString(Object[])} 格式化</li>
 *   <li>否则委托给内部 messageInterpolator 进行标准插值处理</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see NumbersIn
 * @see AbstractMessageInterpolator
 * @see HibernateMessageInterpolatorContext
 * @see Arrays
 * Date: 2020/5/12
 * Time: 12:44
 */
public class NumbersInMessageInterpolator extends AbstractMessageInterpolator {

    private final AbstractMessageInterpolator messageInterpolator;

    public NumbersInMessageInterpolator(AbstractMessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
    }

    @Override
    public String interpolate(Context context, Locale locale, String expression) {
        if (NumbersIn.class.isAssignableFrom(context.getConstraintDescriptor().getAnnotation().getClass())) {
            String resolvedExpression;
            Object variable = getVariable( context, removeCurlyBraces( expression ) );
            if ( variable != null ) {
                if ( variable.getClass().isArray() ) {
                    if (long[].class.isAssignableFrom(variable.getClass())) {
                        resolvedExpression = Arrays.toString( (long[]) variable );
                    } else {
                        resolvedExpression = Arrays.toString( (Object[]) variable );
                    }
                }
                else {
                    resolvedExpression = variable.toString();
                }
            }
            else {
                resolvedExpression = messageInterpolator.interpolate(expression, context, locale);
            }
            return resolvedExpression;
        } else {
            return messageInterpolator.interpolate(expression, context, locale);
        }
    }

    private Object getVariable(Context context, String parameter) {
        if ( context instanceof HibernateMessageInterpolatorContext) {
            Object variable = ( (HibernateMessageInterpolatorContext) context ).getMessageParameters().get( parameter );
            if ( variable != null ) {
                return variable;
            }
        }
        return context.getConstraintDescriptor().getAttributes().get( parameter );
    }

    private String removeCurlyBraces(String parameter) {
        return parameter.substring( 1, parameter.length() - 1 );
    }

}
