package io.hqwu.commons.mybatisplus.pojo;

import com.umpay.commons.annotation.SourceProperty;
import com.umpay.commons.util.converters.Integer2Boolean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/15
 * Time: 21:41
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Area {

    private Integer id;

    private String name;

    private Integer parentId;

    @SourceProperty(name = "childCount", valueOf = Integer2Boolean.class, params = {"true", "0"})
    private Boolean isLeaf;

}
