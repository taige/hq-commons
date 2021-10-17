package io.hqwu.commons.jackson;

/**
 * Created with IntelliJ IDEA for authorization-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-05-04
 * Time: 4:48 p.m.
 */
public interface ValidatedJson {

    default Class<?>[] validateGroups() {
        return null;
    }

}
