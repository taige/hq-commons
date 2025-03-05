package io.hqwu.commons.dubbo.support;

import io.hqwu.commons.util.Formatter;
import io.hqwu.commons.util.StringUtil;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * AccessLog参数对象,包装响应码,功能码,请求流水,响应流水,业务数据1，业务数据2...等数据
 *
 * @author wyshenjianlin <a
 *         href="mailto:wyshenjianlin@chinabank.com.cn">wyshenjianlin@chinabank.com.cn</a> <br>
 *         QQ: 79043549
 * @version 1.0 2014-4-8
 */
public class AccessLogParameters {
    public static final String NULL = "<null>";
    /**
     * 成功响应码
     */
    static final String SUCCESS_RESPONSE_CODE = "200";
    /**
     * 分隔符
     */
    private static final String DELIMITER = ";";
    /**
     * 返回码,默认成功
     */
    private String responseCode = SUCCESS_RESPONSE_CODE;
    /**
     * 响应描述
     */
    private String responseDesc = NULL;
    /**
     * 功能码
     */
    private String funCode = NULL;
    /**
     * 请求流水
     */
    private String requestTrace = NULL;
    /**
     * 响应流水
     */
    private String responseTrace = NULL;
    /**
     * 业务元素列表，简单起见默认扫描顺序
     */
    private LinkedList<Object> businessElements;
    /**
     * 调用开始时间，默认创建时间
     */
    private long invokesStartTime = System.nanoTime();
    /**
     * 调用结束时间，默认-1
     */
    private long invokesEndTime = -1L;

    /**
     * 请求参数
     * @return
     */
    private String requestArgs = NULL;

    public long getInvokesEndTime() {
        return invokesEndTime;
    }

    public void setInvokesEndTime(long invokesEndTime) {
        this.invokesEndTime = invokesEndTime;
    }

    public long getInvokesStartTime() {
        return invokesStartTime;
    }

    public void setInvokesStartTime(long invokesStartTime) {
        this.invokesStartTime = invokesStartTime;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        if (StringUtil.isNotEmpty(responseCode)) {
            this.responseCode = responseCode;
        }
    }

    public void setResponseCode(Object responseCode) {
        if (responseCode != null) {
            this.setResponseCode(responseCode.toString());
        }
    }

    public void setRequestTrace(Object requestTrace) {
        if (requestTrace != null) {
            this.setRequestTrace(requestTrace.toString());
        }
    }

    public String getResponseTrace() {
        return responseTrace;
    }

    public void setResponseTrace(String responseTrace) {
        if (StringUtil.isNotEmpty(responseTrace)) {
            this.responseTrace = responseTrace;
        }
    }

    public void setResponseTrace(Object responseTrace) {
        if (responseTrace != null) {
            this.setResponseTrace(responseTrace.toString());
        }
    }

    public void setResponseDesc(String responseDesc) {
        this.responseDesc = responseDesc;
    }

    public void setResponseDesc(Object responseDesc) {
        if (responseDesc != null) {
            setResponseDesc(responseDesc.toString());
        }
    }

    public String getResponseDesc() {
        return responseDesc;
    }

    public String getFunCode() {
        return funCode;
    }

    public void setFunCode(String funCode) {
        this.funCode = funCode;
    }

    public LinkedList<Object> getBusinessElements() {
        return businessElements;
    }

    public void addBusinessElement(Object value) {
        if (businessElements == null) {
            businessElements = new LinkedList<Object>();
        }
        if (value != null) {
            businessElements.addLast(value);
        } else {
            businessElements.addLast(NULL);
        }
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(128);
        //格式：请求流水,功能码,响应码,响应流水,业务数据1,业务数据2...,耗时(ns)
        buf.append(requestTrace).append(DELIMITER);
        buf.append(funCode).append(DELIMITER);
        buf.append(responseCode).append(DELIMITER);
        buf.append(responseDesc).append(DELIMITER);
        buf.append(responseTrace).append(DELIMITER);
        if (businessElements != null) {
            for (Object businessElement : businessElements) {
                buf.append(businessElement).append(DELIMITER);
            }
        }
        if (invokesEndTime == -1L) {
            invokesEndTime = System.nanoTime();
        }
        buf.append(Formatter.formatNS(invokesEndTime - invokesStartTime)); //.append("ns");
        return buf.toString();
    }

    public void setRequestArgs(Object[] requestArgs) {
        if (requestArgs != null) {
            this.requestArgs = Arrays.stream(requestArgs)
                    .map(this::objectToString)
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    protected String objectToString(Object arg) {
        if (arg == null) {
            return NULL;
        } else if (arg.getClass().isArray()) {
            Class<?> eleType = arg.getClass().getComponentType();
            int len = Array.getLength(arg);
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (boolean.class.equals(eleType)) {
                    sb.append(Array.getBoolean(arg, i));
                } else if (double.class.equals(eleType)) {
                    sb.append(Array.getDouble(arg, i));
                } else if (float.class.equals(eleType)) {
                    sb.append(Array.getFloat(arg, i));
                } else if (char.class.equals(eleType)) {
                    sb.append(Array.getChar(arg, i));
                } else if (byte.class.equals(eleType)) {
                    sb.append(Array.getByte(arg, i));
                } else if (short.class.equals(eleType)) {
                    sb.append(Array.getShort(arg, i));
                } else if (int.class.equals(eleType)) {
                    sb.append(Array.getInt(arg, i));
                } else if (long.class.equals(eleType)) {
                    sb.append(Array.getLong(arg, i));
                } else {
                    sb.append(Array.get(arg, i));
                }
            }
            sb.append(']');
            return sb.toString();
        } else {
            return arg.toString();
        }
    }

    public String getRequestArgs() {
        return requestArgs;
    }

}

