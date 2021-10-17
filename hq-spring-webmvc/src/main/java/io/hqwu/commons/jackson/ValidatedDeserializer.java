package io.hqwu.commons.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/31
 * Time: 11:19
 */
public class ValidatedDeserializer extends BeanDeserializer {

    private final Validator validator;
    private final ViolationExceptionFactory exceptionFactory;

    public ValidatedDeserializer(BeanDeserializerBase deserializer,
                                 Validator validator,
                                 ViolationExceptionFactory exceptionFactory) {
        super(deserializer);
        this.validator = validator;
        this.exceptionFactory = exceptionFactory;
    }

    @Override
    public ValidatedJson deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ValidatedJson request = (ValidatedJson) rawDeserialize(jp, ctxt);
        // validate response
        validate(request);
        return request;
    }

    protected Object rawDeserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return super.deserialize(jp, ctxt);
    }

    protected void validate(ValidatedJson object) {
        if (object instanceof ValidatedJsonResponse) {
            if (((ValidatedJsonResponse) object).hasError()) {
                // 不成功的response，skip validate
                return;
            }
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        if (violations.size() > 0) {
            throw exceptionFactory.newViolationException(violations, object);
        }
        Class<?>[] groups = object.validateGroups();
        if (groups != null && groups.length > 0) {
            violations = validator.validate(object, groups);
            if (violations.size() > 0) {
                throw exceptionFactory.newViolationException(violations, object);
            }
        }
    }

}
