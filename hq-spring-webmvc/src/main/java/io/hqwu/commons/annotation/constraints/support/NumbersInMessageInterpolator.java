package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.NumbersIn;
import org.hibernate.validator.messageinterpolation.HibernateMessageInterpolatorContext;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Arrays;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/12
 * Time: 12:44
 */
public class NumbersInMessageInterpolator extends ParameterMessageInterpolator {

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
                resolvedExpression = expression;
            }
            return resolvedExpression;
        } else {
            return super.interpolate(context, locale, expression);
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
