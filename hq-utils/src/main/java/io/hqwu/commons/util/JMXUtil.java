package io.hqwu.commons.util;

import lombok.experimental.UtilityClass;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * JMX（Java Management Extensions）工具类。
 * <p>
 * 该工具类用于简化 MBean 的注册和注销操作。
 * 主要功能特性：
 * <ul>
 *   <li>MBean 注册：将 MBean 对象注册到平台 MBean 服务器，支持自动覆盖已存在的实例。</li>
 *   <li>MBean 注销：从平台 MBean 服务器中注销指定名称的 MBean。</li>
 *   <li>异常处理：对 JMX 操作中的异常进行统一封装和处理。</li>
 * </ul>
 * </p>
 *
 * @author taige (Wu, Hongqiang)
 * @see ObjectName
 * @see MBeanServer
 * @see ManagementFactory
 * @see InstanceAlreadyExistsException
 * @see JMException
 */
@UtilityClass
public final class JMXUtil {

    public static ObjectName register(String name, Object mbean) {
        try {
            ObjectName objectName = new ObjectName(name);

            MBeanServer mbeanServer = ManagementFactory
                    .getPlatformMBeanServer();

            try {
                mbeanServer.registerMBean(mbean, objectName);
            } catch (InstanceAlreadyExistsException ex) {
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(mbean, objectName);
            }

            return objectName;
        } catch (JMException e) {
            throw new IllegalArgumentException(name, e);
        }
    }

    public static void unregister(String name) {
        try {
            MBeanServer mbeanServer = ManagementFactory
                    .getPlatformMBeanServer();

            mbeanServer.unregisterMBean(new ObjectName(name));
        } catch (InstanceNotFoundException ignored) {
        } catch (JMException e) {
            throw new IllegalArgumentException(name, e);
        }

    }

}
