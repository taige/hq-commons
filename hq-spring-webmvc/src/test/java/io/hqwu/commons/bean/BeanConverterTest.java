package io.hqwu.commons.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.hqwu.commons.annotation.SourceProperty;
import io.hqwu.commons.bean.converters.DateTime2String;
import lombok.*;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.beans.BeanCopier;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/13
 * Time: 20:59
 */
@CustomLog
public class BeanConverterTest {

    @Data
    @Accessors(chain = true)
    public static class Parent {
        private String surName;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Accessors(chain = true)
    @ToString(callSuper = true)
    public static class Child extends Parent {
        private String firstName;
    }

    @Data
    @Accessors(chain = true)
    public static class Src {
        private Child person;
        private String nickname;
        private Long number;
        private String string;
    }

    @Data
//    @Accessors(chain = true)
    public static class Tag {
        private Parent person;
        private String nickname;
        private String number;
        private String string;
    }


    @Test
    void test_copier() {
        {
            Src src = new Src();
            src.setPerson((Child) new Child().setFirstName("hongqiang-1").setSurName("w-1"));
            src.setNickname("taige");
            src.setNumber(123L);
            LOGGER.debug(src);
            BeanCopier copier = BeanCopier.create(Src.class, Tag.class, false);
            Tag tag = BeanUtils.instantiateClass(Tag.class);
            copier.copy(src, tag, null);
            LOGGER.debug(tag);
//            BeanCopier copier1 = BeanCopier.create(Tag.class, Src.class, false);
//            Src src1 = BeanUtils.instantiateClass(Src.class);
//            copier1.copy(tag, src1, null);
//            LOGGER.debug(src1);
        }
        {
            Src src = new Src();
            src.setPerson((Child) new Child().setFirstName("hongqiang-2").setSurName("wu-2"));
            src.setNickname("taige");
            src.setNumber(124L);
            LOGGER.debug(src);
            Tag tag = BeanConverter.convert(src, Tag.class);
            LOGGER.debug(tag);
//            Src src1 = BeanConverter.convert(tag, Src.class);
//            LOGGER.debug(src1);
        }
        {
            Src src = new Src();
            src.setPerson((Child) new Child().setFirstName("hongqiang-3").setSurName("wu-3"));
            src.setNickname("taige");
            src.setNumber(125L);
            LOGGER.debug(src);
            Tag tag = BeanConverter.convert(src, Tag.class);
            LOGGER.debug(tag);
//            Src src1 = BeanConverter.convert(tag, Src.class);
//            LOGGER.debug(src1);
        }
    }

    @Test
    void test_convert_list() {

        TInvoiceItem tInvoiceItem = new TInvoiceItem()
                .setId(1)
                .setInvoiceId(2)
                .setProdCategory("payment001")
                .setProdName("abcdef")
                .setInsertTime(LocalDateTime.of(2020, 5, 5, 12, 12, 12));
        TInvoice tInvoice = new TInvoice()
                .setInvoiceId(2)
                .setBuyerId("12")
                .setItems(Collections.singletonList(tInvoiceItem));

        Invoice invoice = BeanConverter.convert(tInvoice, Invoice.class);
        assertNotNull(invoice);
        assertNotNull(invoice.getItems());
        assertEquals(1, invoice.getItems().size());
        assertEquals(1, invoice.getItems().get(0).getItemId());
        assertEquals(2, invoice.getItems().get(0).getInvoiceId());
        assertEquals("abcdef", invoice.getItems().get(0).getProdName());
        assertEquals("2020-05-05 12:12:12", invoice.getItems().get(0).getCreateTime());
        assertEquals("payment001", invoice.getItems().get(0).getProdCategory());
        assertEquals(2, invoice.getInvoiceId());
        assertEquals("12", invoice.getBuyerId());
        LOGGER.debug(invoice);
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @TableName("gpf_invoice")
    static class TInvoice implements Serializable {

        //    @Setter(AccessLevel.NONE)
        @TableId(value="id", type= IdType.AUTO)
        private Integer invoiceId;

        private String invoiceNo;

        private String invoiceCode;

        private String buyerId;

        private transient String buyerName;

        private transient String merTaxId;

        private transient String merBankName;

        private transient String merBankAccount;

        private transient String merAddress;

        private transient String merPhone;

        private transient String sellerName;

        private transient List<TInvoiceItem> items;

        private String invoiceType;

        private String invoiceForm;

        private Integer invoiceStatus;

        private BigDecimal totalAmount;

        private String invoiceNotes;

        @SourceProperty("issueTime")
        private LocalDate issueDate;

        private String issuerName;

        private String auditorName;

        private String payeeName;

        private transient String eInvoiceUrl;

        private Integer operatorId;

        private transient String operatorName;

        private LocalDateTime insertTime;

        private LocalDateTime updateTime;


    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @NoArgsConstructor
    @TableName("gpf_invoice_item")
    static class TInvoiceItem {

        @TableId(value = "id", type = IdType.AUTO)
        private Integer id;

        private Integer invoiceId;

        private String prodCategory;

        private BigDecimal totalAmount;

        private BigDecimal untaxAmount;

        private BigDecimal tax;

        private Integer prodQuantity;

        private BigDecimal unitPrice;

        private transient String prodName;

        private transient Integer taxRate;

        private transient String prodSpecMode;

        private transient String prodUnit;

        private LocalDateTime insertTime;

    }

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @Data
    static class InvoiceItem extends Product {

        @SourceProperty(name = "id")
        private Integer itemId;

        private Integer invoiceId;

        @SourceProperty
        private Double totalAmount;

        @SourceProperty
        private Double untaxAmount;

        @SourceProperty
        private Double tax;

        private Integer prodQuantity;

        @SourceProperty
        private Double unitPrice;

        @SourceProperty(ignore = true)
        private String updateTime;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Invoice {

        @NotNull
        @NotBlank
        private Integer invoiceId;

        @NotNull
        @NotBlank
        private String invoiceNo;

        @NotNull
        @NotBlank
        private String invoiceCode;

        @NotBlank
        @Size(min=8, max=32)
        private String buyerId;

        @NotBlank
        @Size(min=2, max=100)
        private String buyerName;

        private String merTaxId;

        @Size(max=64)
        private String merBankName;

        @Size(max=32)
        private String merBankAccount;

        @Size(max=90)
        private String merAddress;

        @Size(min=8, max=20)
        private String merPhone;

        private String sellerName;

        @Pattern(regexp = "S|G|N")
        private String invoiceType;

        private String invoiceForm;

//        @NumbersIn({-1, 1})
        private Integer amountDirection;

//        @NumbersIn({0, 1})
        private Integer invoiceStatus;

        private List<InvoiceItem> items;

        private String invocieNotes;

        private String issuerName;

        private String auditorName;

        private String payeeName;

        @SourceProperty(valueOf = DateTime2String.class)
        private String issueDate;

        private String eInvoiceUrl;

        @SourceProperty(name = "insertTime", valueOf = DateTime2String.class)
        private String createTime;

        @SourceProperty(name = "operatorId")
        private String lastOperatorId;

        private String operatorName;

        @SourceProperty(name = "updateTime", valueOf = DateTime2String.class)
        private String lastOperationTime;


    }

    @Data
    static class Product {

        @NotBlank
        @Max(32)
        private String prodCategory;

        @NotBlank
        @Max(32)
        private String prodName;

        private String taxCodeVer;

        @NotBlank
        @Max(32)
        private String taxCateCode;

        @NotNull
        @Max(4)
        private Integer taxRate;

        /**
         * 规格型号(ProductDetail)
         */
        @Max(32)
        private String prodSpecMode;

        /**
         * 单位
         */
        @Max(4)
        private String prodUnit;

        /**
         * 0 - ineligible; 1 - eligible
         */
//        @NumbersIn({0})
        private Integer taxPreFlag = 0;

        @Null
        private String taxPreCon = null;

        @Null
        private String customizeCode = null;

        /**
         * 空：非零税率，0：出口退税，1：免税，2：不征收，3 普通零税率
         */
        @Null
        private String zeroTaxFlag = null;

        private Integer operatorId;

        private Integer operatorName;

        //    private LocalDateTime insertTime;

        @SourceProperty(name = "insertTime", valueOf = DateTime2String.class)
        private String createTime;

        @SourceProperty(valueOf = DateTime2String.class)
        private String updateTime;

    }

}
