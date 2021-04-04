package io.hqwu.commons.mybatisplus;

import io.hqwu.commons.mybatisplus.entity.TAreas;
import io.hqwu.commons.mybatisplus.pojo.Area;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Created with IntelliJ IDEA for pp-gopay-fa
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2020/5/15
 * Time: 22:24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AreaQuery extends AbstractQueryRequest<TAreas, Area> {

}
