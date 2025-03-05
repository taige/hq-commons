package io.hqwu.commons.dubbo.filter;

import io.hqwu.commons.dubbo.support.AccessLogParameters;
import io.hqwu.commons.util.Formatter;
import io.hqwu.commons.util.Logger;
import io.hqwu.commons.util.LoggerFactory;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 记录Dubbo Service的AccessLog，用于系统监控
 * <p/>
 //日志格式:
 //0.     [CLIENT-current-thread-name] / [SERVER-current-thread-name]
 //1.     当前时间: yyyy-MM-dd.HH:mm:ss.SSS;
 //2.     地址信息: clientHost:clientPort->serverHost:serverPort;
 //3.     请求服务: [group/]serviceName[:version].methodName(paraTypes,...);
 //4.     请求时间: yyyy-MM-dd.HH:mm:ss.SSS;
 //  ++++ 请求数据、响应数据全部由服务提供方定义 ++++
 //5.     请求数据: 请求流水;
 //6~N-1. 响应数据: ERROR:xxx / EXCEPTION:xxx / RESULT:<null> / <object> ;
 //N.     耗时(ns): xxx;
 * <p/>
 * @author ustbsjl
 * @author taige.wu
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER}, order = 20)
public class AccessLogFilter implements Filter {
    private static final Logger LOGGER = new Logger();

    private static final String ACCESS_LOG_KEY = "ACCESS.dubbo";

    private static final String MESSAGE_DATE_FORMAT = "yyyy-MM-dd.HH:mm:ss.SSS";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(MESSAGE_DATE_FORMAT);

    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        Logger accessLogger = LoggerFactory.getLogger(ACCESS_LOG_KEY + "." + invoker.getInterface().getName());
        // level配置在INFO以上，或者在调用com.alibaba.dubbo.rpc.service.EchoService.$echo() 时，禁止输出Access Log
        if (! accessLogger.isInfoEnabled()
                || (inv.getMethodName().equals(Constants.$ECHO) && inv.getArguments() != null && inv.getArguments().length == 1)) {
            return invoker.invoke(inv);
        }

        SgmAccessLog accessLog = new SgmAccessLog(invoker, inv);
        try {
            Result result = invoker.invoke(inv);
            accessLog.setResult(result);
            return result;
        } catch (RpcException e) {
            accessLog.setRpcError(e);
            throw e;
        } finally {
            accessLogger.info(accessLog.dumpLog());
        }
    }

    /**
     *
     *  组装请求信息
     *
     * @param accessLog
     * @param invoker
     * @param inv
     */
    protected void buildRequestLog(SgmAccessLog accessLog, Invoker<?> invoker, Invocation inv) {
        RpcContext context = RpcContext.getContext();

        if (accessLog.isClientSide()) {
            accessLog.setClientHost(context.getLocalHost());
            accessLog.setClientPort(context.getLocalPort());
            accessLog.setServerHost(context.getRemoteHost());
            accessLog.setServerPort(context.getRemotePort());
        } else {
            accessLog.setClientHost(context.getRemoteHost());
            accessLog.setClientPort(context.getRemotePort());
            accessLog.setServerHost(context.getLocalHost());
            accessLog.setServerPort(context.getLocalPort());
        }

        accessLog.setGroup(invoker.getUrl().getParameter(CommonConstants.GROUP_KEY));
        accessLog.setServiceName(invoker.getInterface().getName());
        accessLog.setVersion(invoker.getUrl().getParameter(CommonConstants.VERSION_KEY));
        accessLog.setMethodName(inv.getMethodName());
        Class<?>[] requestParamTypes = inv.getParameterTypes();
        if (requestParamTypes != null && requestParamTypes.length > 0) {
            for (Class<?> type : requestParamTypes) {
                accessLog.appendRequestParam(type);
            }
        }

        accessLog.setRequestArgs(inv.getArguments());
    }

    /**
     * 根据异常类型，得到不同返回码
     *
     * @param e RpcException
     * @return 返回码
     */
    protected String getExceptionDesc(Throwable e) {
        Throwable cause = e.getCause();
        if (cause != null && cause != e) {
            return getExceptionDesc(cause);
        } else if (e instanceof RpcException) {
            switch (((RpcException) e).getCode()) {
                case RpcException.UNKNOWN_EXCEPTION:
                    return "UNKNOWN_EXCEPTION";
                case RpcException.NETWORK_EXCEPTION:
                    return "NETWORK_EXCEPTION";
                case RpcException.TIMEOUT_EXCEPTION:
                    return "TIMEOUT_EXCEPTION";
                case RpcException.BIZ_EXCEPTION:
                    return "BIZ_EXCEPTION";
                case RpcException.FORBIDDEN_EXCEPTION:
                    return "FORBIDDEN_EXCEPTION";
                case RpcException.SERIALIZATION_EXCEPTION:
                    return "SERIALIZATION_EXCEPTION";
                case RpcException.NO_INVOKER_AVAILABLE_AFTER_FILTER:
                    return "NO_INVOKER_AVAILABLE_AFTER_FILTER";
                case RpcException.LIMIT_EXCEEDED_EXCEPTION:
                    return "LIMIT_EXCEEDED_EXCEPTION";
                case RpcException.TIMEOUT_TERMINATE:
                    return "TIMEOUT_TERMINATE";
                default:
                    return SgmAccessLog.NULL;
            }
        } else {
            return e.toString();
        }
    }

    private class SgmAccessLog extends AccessLogParameters {

        private boolean clientSide;

        private String group;

        private String serviceName;

        private String methodName;

        private String version;

        private StringBuilder requestParam;

        private String threadName = Thread.currentThread().getName();

        private String clientHost;

        private int clientPort;

        private String serverHost;

        private int serverPort;

        private Throwable rpcError;

        private Result result;

        private LocalDateTime startTime = LocalDateTime.now();

        /**
         * 分隔符
         */
        private static final String DELIMITER = ";";

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        SgmAccessLog(Invoker<?> invoker, Invocation inv) {
            try {
                this.clientSide = CommonConstants.CONSUMER.equals(invoker.getUrl().getParameter(CommonConstants.SIDE_KEY));
                buildRequestLog(this, invoker, inv);
            } catch (Throwable t) {
                LOGGER.warn("Exception in buildRequestLog of service(" + invoker + " -> " + inv + ")", t);
            }
        }

        /**
         //日志格式:
         //0.     [CLIENT-current-thread-name] / [SERVER-current-thread-name]
         //1.     当前时间: yyyy-MM-dd.HH:mm:ss.SSS;
         //2.     地址信息: clientHost:clientPort->serverHost:serverPort;
         //3.     请求服务: [group/]serviceName[:version].methodName(paraTypes,...);
         //4.     请求时间: yyyy-MM-dd.HH:mm:ss.SSS;
         //  ++++ 请求数据、响应数据全部由服务提供方定义 ++++
         //5.     请求数据: [<object>];
         //6~N-1. 响应数据: ERROR:xxx / EXCEPTION:xxx / RESULT:<null> / <object> ;
         //N.     耗时(ns): xxx;
         *
         * @return
         */
        public String dumpLog() {
            try {
                return _dumpLog();
            } catch (Throwable t) {
                LOGGER.warn("Exception in dumpLog", t);
                return "Exception in dumpLog" + t;
            }
        }

        private String _dumpLog() {
            final StringBuilder buf = new StringBuilder(256);
            //0.     [CLIENT-current-thread-name] / [SERVER-current-thread-name]
            buf.append(clientSide ? "[CLIENT-" : "[SERVER-").append(threadName).append("]").append(DELIMITER);
            //1.     当前时间: yyyy-MM-dd.HH:mm:ss.SSS;
            buf.append(dateTimeFormatter.format(LocalDateTime.now())).append(DELIMITER);
            //2.     地址信息: clientHost:clientPort->serverHost:serverPort;
            buf.append(clientHost).append(":").append(clientPort)
                    .append("->").append(serverHost).append(":").append(serverPort).append(DELIMITER);
            //3.     请求服务: [group/]serviceName[:version].methodName(paraTypes,...);
            if (null != group && group.length() > 0) {
                buf.append(group).append("/");
            }
            buf.append(serviceName);
            if (null != version && version.length() > 0) {
                buf.append(":").append(version);
            }
            buf.append(".").append(methodName).append("(");
            if (requestParam != null) {
                buf.append(requestParam);
            }
            buf.append(")").append(DELIMITER);
            //4.     请求时间: yyyy-MM-dd.HH:mm:ss.SSS;
            buf.append(dateTimeFormatter.format(startTime)).append(DELIMITER);
            //5.     请求数据: 请求流水;
            buf.append(getRequestArgs()).append(DELIMITER);
            //6~N-1. 响应数据: ERROR:xxx / EXCEPTION:xxx / 响应码;响应流水;请求业务数据1;请求业务数据2;...;响应业务数据1;响应业务数据2;...
            if (rpcError != null) {
                buf.append("ERROR:").append(getExceptionDesc(rpcError)).append(DELIMITER);
            } else if (result != null) {
                if (result.hasException()) {
                    buf.append("EXCEPTION:").append(getExceptionDesc(result.getException())).append(DELIMITER);
                } else {
                    buf.append(objectToString(result.getValue())).append(DELIMITER);
                }
            } else {
                buf.append("RESULT:").append(NULL).append(DELIMITER);
            }
            //N.     耗时(ns): xxx;
            buf.append(Formatter.formatNS(getNanoTime())); //.append("ns");

            return buf.toString();
        }

        public void setClientHost(String clientHost) {
            this.clientHost = clientHost;
        }

        public void setClientPort(int clientPort) {
            this.clientPort = clientPort;
        }

        public void setServerHost(String serverHost) {
            this.serverHost = serverHost;
        }

        public void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void appendRequestParam(Class<?> type) {
            if (requestParam == null) {
                requestParam = new StringBuilder();
            } else {
                requestParam.append(",");
            }
            requestParam.append(type.getSimpleName());
        }

        public void setRpcError(Throwable rpcError) {
            this.rpcError = rpcError;
        }

        public void setResult(Result result) {
            this.result = result;
            this.setInvokesEndTime(System.nanoTime());
        }

        public boolean isClientSide() {
            return clientSide;
        }

        public long getNanoTime() {
            long invokesEndTime = getInvokesEndTime();
            if (invokesEndTime == -1L) {
                invokesEndTime = System.nanoTime();
                setInvokesEndTime(invokesEndTime);
            }
            return invokesEndTime - getInvokesStartTime();
        }
    }
}