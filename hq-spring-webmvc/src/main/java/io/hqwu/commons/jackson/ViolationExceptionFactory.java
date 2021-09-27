package io.hqwu.commons.jackson;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-09-23
 * Time: 20:43
 */
@FunctionalInterface
public interface ViolationExceptionFactory {

    RuntimeException newViolationException(Set<? extends ConstraintViolation<?>> constraintViolations, ValidatedJson violatedJson);

}
