package io.hqwu.commons.jackson;

/**
 * Created with IntelliJ IDEA for authorization-server
 *
 * @author taige (Wu, Hongqiang)
 * Date: 2021-05-05
 * Time: 9:49 a.m.
 */
public interface ValidatedJsonResponse extends ValidatedJson {

    boolean hasError();

}
