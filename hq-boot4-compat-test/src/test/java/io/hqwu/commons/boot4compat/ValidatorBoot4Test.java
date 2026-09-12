package io.hqwu.commons.boot4compat;

import io.hqwu.commons.annotation.constraints.Base64;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hq-spring-webmvc 的自定义约束在 SB4 自带的 Hibernate Validator 9 下是否还能被发现并执行。
 */
@SpringBootTest
class ValidatorBoot4Test {

    @Autowired
    private Validator validator;

    @Test
    void customConstraintRunsOnHibernateValidator9() {
        Base64Pojo ok = new Base64Pojo();
        ok.value = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        assertThat(validator.validate(ok)).isEmpty();

        Base64Pojo bad = new Base64Pojo();
        bad.value = "not base64 !@#";
        Set<ConstraintViolation<Base64Pojo>> violations = validator.validate(bad);
        assertThat(violations).hasSize(1);
    }

    static class Base64Pojo {
        @Base64
        String value;
    }
}
