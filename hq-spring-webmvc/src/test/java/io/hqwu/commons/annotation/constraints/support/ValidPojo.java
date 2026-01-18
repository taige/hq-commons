package io.hqwu.commons.annotation.constraints.support;

import io.hqwu.commons.annotation.constraints.AnyFieldsValidated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import static io.hqwu.commons.annotation.constraints.AnyFieldsValidated.AnyField;

/**
 * Created with IntelliJ IDEA for hq-commons-parent
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-11-16
 * Time: 17:04
 */
@Data
@Accessors(chain = true)
@AnyFieldsValidated(constraint = NotEmpty.class)
@AnyFieldsValidated(constraint = NotBlank.class, groups = ValidPojo.AlipayConstraints.class)
@AnyFieldsValidated(constraint = NotBlank.class, groups = ValidPojo.WechatConstraints.class)
@AnyFieldsValidated(constraint = NotBlank.class, groups = ValidPojo.UnionPayConstraints.class)
public class ValidPojo {

    @AnyField
    private String dftValidStr1;

    @AnyField
    private String dftValidStr2;

    private String str3;

    @AnyField(groups = AlipayConstraints.class)
    private String aliValidStr4;

    @AnyField(groups = {AlipayConstraints.class, WechatConstraints.class})
    private String aliWxStr5;

    @AnyField(groups = {UnionPayConstraints.class, WechatConstraints.class})
    private String upWxStr6;

    @AnyField(groups = UnionPayConstraints.class)
    private String upValidStr7;

    @AnyField(groups = {UnionPayConstraints.class, AlipayConstraints.class})
    private String upAliStr8;

    @AnyField(groups = {WechatConstraints.class})
    private String wxValidStr9;

    public interface AlipayConstraints {}

    public interface WechatConstraints {}

    public interface UnionPayConstraints {}

}
