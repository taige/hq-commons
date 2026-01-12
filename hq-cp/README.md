# hq-cp 模块使用说明

本文档介绍了 hq-cp 模块的简要介绍、如何通过 Maven 与 Gradle 引入依赖、HqcpConfig 中各配置参数的说明以及在 单体应用、Spring、Spring Boot 环境下的使用示例。

-  [1. 简要介绍](#1-简要介绍)
-  [2. 依赖引入](#2-依赖引入)
    - [Maven 引入示例](#maven-引入示例)
    - [Gradle 引入示例](#gradle-引入示例)
-  [3. HqcpConfig 配置参数说明](#3-hqcpconfig-配置参数说明)
-  [4. 在 Spring 环境下的使用示例](#4-在-spring-环境下的使用示例)
    - [4.1 传统 XML 配置方式](#41-传统-xml-配置方式)
    - [4.2 注解配置方式](#42-注解配置方式)
-  [5. 在非 Spring 应用中的手工装配数据源示例](#5-在非-spring-应用中的手工装配数据源示例)
-  [6. 在 Spring Boot 应用中自动装配数据源](#6-在-spring-boot-应用中自动装配数据源)
    - [6.1 Maven 配置](#61-maven-配置)
    - [6.2 Gradle 配置](#62-gradle-配置)
    - [6.3 application.yml 示例配置](#63-applicationyml-示例配置)
-  [7. 相关日志说明](#7-相关日志说明)
    - [7.1 连接池状态日志](#71-连接池状态日志)
      - [7.1.1 配置信息输出](#711-配置信息输出)
      - [7.1.2 连接创建与销毁](#712-连接创建与销毁)
      - [7.1.3 连接检出与回收](#713-连接检出与回收)
      - [7.1.4 Connection 对象方法的调用](#714-Connection-对象方法的调用)
      - [7.1.5 连接池状态信息](#715-连接池状态信息)
      - [7.1.6 其他常见告警信息](#716-其他常见告警信息)
    - [7.2 SQL执行日志](#72-SQL执行日志)
- [总结](#总结)


---

## 1. 简要介绍

hq-cp 模块旨在提供一种符合 Java 标准的高效、可监控且易于维护的数据库连接池实现，以满足高并发场景下对数据库连接管理的需求。其主要特点包括：

- **标准接口实现**：hq-cp 模块实现了 Java 标准的数据库连接获取方式 `DataSource` 接口，使其能够无缝对接各类接口的应用和框架。
- **高性能**：在设计和实现上注重性能优化，其表现足以与主流连接池（如 `HikariCP`）媲美。
- **丰富的日志支持**：提供详细的 SQL 执行日志、性能日志以及告警日志，帮助开发者实时监控数据库操作及连接池状态，提高故障排查与性能调优的效率。
- **灵活的配置**：通过 `HqcpConfig` 类或 `jdbc.properties` 文件，灵活配置各项参数，满足不同业务场景的需求。
- **多数据库支持**：兼容常见数据库如 MySQL、Oracle、DB2 等，方便在多种实际环境中使用。

总的来说，hq-cp 模块不仅能在高并发场景下稳定运行，同时也为开发者提供了丰富的监控和调优手段，并且能作为标准的数据库连接数据源在各类 Java 应用中使用。

---

## 2. 依赖引入

### Maven 引入示例

在项目的 `pom.xml` 文件中添加以下依赖：

```xml
<properties>
  <hq-commons.version>1.4.0</hq-commons.version>
</properties>

<dependencies>
  <!-- 引入 hq-cp 模块 -->
  <dependency>
    <groupId>io.hqwu.commons</groupId>
    <artifactId>hq-cp</artifactId>
    <version>${hq-commons.version}</version>
  </dependency>
</dependencies>
```

### Gradle 引入示例

在 `build.gradle` 文件中添加以下依赖：

```groovy
ext {
    hqCommonsVersion = '1.4.0'
}

dependencies {
    implementation 'io.hqwu.commons:hq-cp:$hqCommonsVersion'
}
```

> 注意：版本号请根据实际发布的版本进行调整。

---

## 3. HqcpConfig 配置参数说明

`HqcpConfig` 类是用于定义 hq-cp 连接池配置参数的核心类。下面说明其中一些常用的配置参数：

| 配置项                     | properties文件的key               | 默认值   | 取值范围               | 说明                                                                                                                                                                                                                     |
|-------------------------|--------------------------------|-------|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| url                     | jdbc.url                       | 无     | 合法的 JDBC URL       | 数据库连接的 URL，用于建立与数据库之间的连接。                                                                                                                                                                                              |
| driverClassName         | jdbc.driver-class-name         | 无     | 非空字符串              | 数据库驱动类的全限定名，用于加载对应的 JDBC 驱动。<br/>* 如果未配置，将自动根据 url 中的数据库类型设置相应的驱动类：<br/>- `oracle`: `oracle.jdbc.driver.OracleDriver` <br/>- `mysql`: `com.mysql.jdbc.Driver`                                                          |
| username                | jdbc.username                  | 无     | 非空字符串              | 数据库登录时使用的用户名。                                                                                                                                                                                                          |
| password                | jdbc.password                  | 无     | 字符串（可为空）           | 数据库登录时使用的密码。                                                                                                                                                                                                           |
| minConnections          | jdbc.min-connections           | 1     | 0 ~ 100            | 连接池中保持的最小连接数。                                                                                                                                                                                                          |
| maxConnections          | jdbc.max-connections           | 10    | 1 ~ 1000           | 连接池允许的最大连接数。                                                                                                                                                                                                           |
| maxStatements           | jdbc.max-statements            | 100   | 10 ~ 1000          | 池中最多缓存的 Statement 数。<br/><b>* 超过该值可能会抛出 SQLException，提示同时创建过多 Statement 而未及时关闭。</b>                                                                                                                                    |
| maxPreStatements        | jdbc.max-pre-statements        | 10    | 5 ~ 200            | 池中最多缓存的 PreparedStatement 数；超过该值时会关闭最早未使用的 PreparedStatement。                                                                                                                                                          |
| idleTimeoutSec          | jdbc.idle-timeout-sec          | 300   | 10 ~ 3600（单位：秒）    | 空闲超时时间，连接空闲超过该时间将被检测（调用`checkStatement`）或销毁（如果当前连接数超过`minConnections`）。                                                                                                                                                |
| checkoutTimeoutMillisec | jdbc.checkout-timeout-millisec | 10000 | -1 ~ 600000（单位：毫秒） | （没有可用连接时，）等待可用连接的超时时间。<br/>- `-1` 无限等待 <b>危险配置，不建议</b><br/>- &nbsp;&nbsp;`0` 立即失败（抛SQLExceptin） <b>也不建议</b><br/>                                                                                                       |
| lifetimeSec             | jdbc.lifetime-sec              | 0     | 0 ~ 86400（单位：秒）    | 连接存活时间，超过该时间的连接将被销毁。 <br/>- `0` 不回收 <br/>非`0`时，取与 `idleTimeoutSec` 两者中取较大值。                                                                                                                                            |
| verbose                 | jdbc.verbose                   | false | true 或 false       | 是否记录除 SQL 语句及执行时间外的其他日志信息。                                                                                                                                                                                             |
| printSql                | jdbc.print-sql                 | true  | true 或 false       | 是否记录 SQL 语句及其执行时间。                                                                                                                                                                                                     |
| infoSqlThreshold        | jdbc.info-sql-threshold        | 10    | 整数                 | 当 `printSql` 为 `true` 且 SQL 执行耗时超过该阈值（单位：毫秒）时，打印 `INFO` 级别日志。 <br/>- `≤0` 不打印 `INFO` 级别日志                                                                                                                              |
| warnSqlThreshold        | jdbc.warn-sql-threshold        | 100   | 整数                 | 当 `printSql` 为 `true` 且 SQL 执行耗时超过该阈值（单位：毫秒）时，打印 `WARN` 级别日志。<br/>- `≤0` 不打印 `WARN` 级别日志 <br/>- `>0` 时，取与`infoSqlThreshold`两者中的较大值                                                                                     |
| checkStatement          | jdbc.check-statement           | 无     | 合法的 SQL 查询语句       | 检测连接是否可用的 SQL 查询语句。<br/>* 如果未配置，将自动根据 url 中的数据库类型设置相应的检测语句（未匹配到以下数据库类型，则无法检测 <b>!!不建议!!</b>）：<br/>- `oracle`: `select systimestamp from dual` <br/>- `mysql`: `select now()` <br/>- `db2`: `values(current timestamp)` |
| jmxLevel                | jdbc.jmx-level                 | 0     | 0、1 或 2            | JMX 管理级别：<br/>- `0` 表示不启用 JMX <br/>- `1` 表示管理 `Hqcp`连接池 及其 `HqcpConfig`配置 实例 <br/>- `2` 表示管理 `数据库连接` 实例                                                                                                                |
| transactionMode         | jdbc.transaction-mode          | false | true 或 false       | 获取连接（调用 `DataSource.getConnection()` 方法）时是否返回的`Connection`对象是否默认设置为事务模式：<br/>- `true` 表示使用事务模式，即 `autoCommit`=`false` <br/>- `false` 表示非事务模式，即 `autoCommit`=`true`  <br/>* 使用Spring管理事务时可忽略该配置                         |
| commitOnClose           | jdbc.commit-on-close           | false | true 或 false       | 关闭连接时（`autoCommit`=`false`，且有未提交的语句时）是否自动提交事务。<br/>- `true` 自动commit <br/>- `false` 自动rollback <br/>* 使用Spring管理事务时可忽略该配置                                                                                              |
| lazyInit                | jdbc.lazy-init                 | false | true 或 false       | 是否延迟初始化连接池。<br/>- `true` 将在监控线程中建立连接 <br/>- `false` 在主线程（调用 `DataSource.getConnection()`的线程）中建立连接                                                                                                                      |

> 更多详细的配置参数请参考 `HqcpConfig` 类的源码文档说明。

---

## 4. 在 Spring 环境下的使用示例

下面介绍两种配置方式，展示如何在 Spring 环境下装配hq-cp数据源。

### 4.1 传统 XML 配置方式

通过 Spring 的 XML 文件为 HqcpDataSource 配置各项属性，如下所示：

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">
    <!-- 装配 hq-cp 数据源 Bean -->
    <bean id="dataSource" class="io.hqwu.commons.cp.HqcpDataSource" 
            init-method="init" destroy-method="shutdown">
        <property name="initOnStartup" value="true"/>
        <!-- 各项属性配置，参考 3. HqcpConfig 配置参数说明 -->
        <property name="url" value="jdbc:your_url"/>
        <property name="driverClassName" value="your.driver.ClassName"/>
        <property name="username" value="your_username"/>
        <property name="password" value="your_password"/>
        <property name="minConnections" value="1"/>
        <property name="maxConnections" value="10"/>

        <!-- 根据需要可配置其他属性 -->
    </bean>
</beans>
```

### 4.2 注解配置方式

如果项目环境支持 Spring 3.0 或以上版本，则可以采用基于注解的配置方式来装配数据源，如下所示：

```java
import io.hqwu.commons.cp.HqcpDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public DataSource hqcpDataSource() {
        HqcpDataSource dataSource = new HqcpDataSource();
        // 在启动时进行初始化
        dataSource.setInitOnStartup(true);
        // 各项属性配置，参考 3. HqcpConfig 配置参数说明
        dataSource.setUrl("jdbc:your_url");
        dataSource.setDriverClassName("your.driver.ClassName");
        dataSource.setUsername("your_username");
        dataSource.setPassword("your_password");
        dataSource.setMinConnections(1);
        dataSource.setMaxConnections(10);

        // 根据需要可设置其他属性，比如：
        return dataSource;
    }
}
```

以上两种方式可以根据项目需求和团队习惯选择使用。

---

## 5. 在非 Spring 应用中的手工装配数据源示例

在没有使用 Spring 这样的 IoC 容器的场景下，可以通过直接实例化和配置数据源来手工装配数据库连接。

下面是一个示例，展示如何使用 hq-cp 数据源进行手工配置和初始化：

```java
import io.hqwu.commons.cp.HqcpDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ManualDataSourceSetup {
    public static void main(String[] args) {
        // 创建并配置数据源实例
        HqcpDataSource dataSource = new HqcpDataSource();
        dataSource.setInitOnStartup(true);
        dataSource.setUrl("jdbc:your_url");
        dataSource.setDriverClassName("your.driver.ClassName");
        dataSource.setUsername("your_username");
        dataSource.setPassword("your_password");
        dataSource.init();

        // 注册 JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dataSource.shutdown();
                System.out.println("数据源已成功关闭。");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // 执行业务逻辑或连接测试
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null) {
                System.out.println("数据源装配并连接成功！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 在此处执行其他业务逻辑，程序结束时 JVM 会自动调用 Shutdown Hook 执行 shutdown() 方法
        
    }
}
```

这种配置方式适用于非 Spring 环境下的场景，可以帮助开发者更灵活地控制数据源的初始化和使用过程。

---

## 6. 在 Spring Boot 应用中自动装配数据源

在 Spring Boot 环境下，我们可以引入 hq-cp-boot-starter 模块，利用 Spring Boot 的自动装配功能对数据源进行快速配置。

只需添加启动器依赖，然后在 application.yml 中给出相关配置，模块就会自动创建并初始化 HqcpDataSource。

需要注意的是，`url` `driver-class-name` `username` `password` 4个参数的配置会优先读取 `spring.datasource.hqcp` 下的配置属性；
如果没有找到相应配置，则会自动使用 `spring.datasource` 下的属性作为默认配置。


### 6.1 Maven 配置

在 Maven 项目的 pom.xml 文件中增加如下依赖：

```xml
<properties>
  <hq-commons.version>1.4.0</hq-commons.version>
</properties>

<dependencies>
  <!-- 引入 hq-cp-boot-starter 模块，实现数据源的自动装配 -->
  <!-- hq-cp-boot-starter 依赖了 hq-cp，可以不再显式引入 --> 
  <dependency>
    <groupId>io.hqwu.commons</groupId>
    <artifactId>hq-cp-boot-starter</artifactId>
    <version>${hq-commons.version}</version>
  </dependency>
</dependencies>
```

### 6.2 Gradle 配置

在 Gradle 项目的 build.gradle 文件中添加以下依赖：

```groovy
ext {
    hqCommonsVersion = '1.4.0' 
}

dependencies {
    // 引入 hq-cp-boot-starter 模块，实现数据源自动装配
    implementation "io.hqwu.commons:hq-cp-boot-starter:$hqCommonsVersion"
}
```

### 6.3 application.yml 示例配置

在 application.yml 中对数据源进行配置，示例配置如下：

```yaml
spring:
  datasource:
    # 基础数据源配置
    url: jdbc:your_url
    driver-class-name: your.driver.ClassName
    username: your_username
    password: your_password
    hqcp:
      # hq-cp 专用配置
      # 若在此处配置了url/driver-class-name/username/password，则会覆盖基础配置的相应设置
      init-on-startup: true
      min-connections: 1
      max-connections: 10
```

通过以上配置，Spring Boot 应用在启动时即可自动装配 HqcpDataSource，实现数据源的高效管理，简化了业务开发中的环境配置工作。

---

## 7. 相关日志说明

hq-cp提供了丰富的日志，用以监控、调优连接池、连接的使用情况、配置情况等。

### 7.1 连接池状态日志

#### 7.1.1 配置信息输出
在连接池初始化时，会依次打印各个配置项，日志示例如下：
```
[INFO] url                     = 'jdbc:oracle:thin:@localhost:1521:orcl'
[INFO] username                = 'testuser'
[INFO] password                = '******'
[INFO] minConnections          = 1
[INFO] maxConnections          = 10
[INFO] maxStatements           = 100
[INFO] maxPreStatements        = 10
[INFO] idleTimeoutSec          = 300
[INFO] checkoutTimeoutMillisec = 10000
[INFO] lifetimeSec             = 0
[INFO] commitOnClose           = false
[INFO] verbose                 = true
[INFO] printSql                = false
[INFO] checkStatement          = 'select systimestamp from dual'
[INFO] lazyInit                = false
[INFO] infoSqlThreshold        = 10
[INFO] warnSqlThreshold        = 100
[INFO] queryTimeout            = 60
[INFO] jmxLevel                = 2
[INFO] transactionMode         = false
```

#### 7.1.2 连接创建与销毁
与数据库服务器的物理连接被创建和断开时，会记录相关信息：
```
[INFO] HQCP#0#0 make new connection to jdbc:oracle:thin:@localhost:1521:orcl use 1,304,500 ns
[INFO] HQCP#0 +)4 connections to jdbc:oracle:thin:@localhost:1521:orcl

其他日志...

[INFO] HQCP#0#0 real closed.
[INFO] HQCP#0 -)3 connections to jdbc:oracle:thin:@localhost:1521:orcl
```
其中：
- `HQCP#0` 表示连接池唯一ID。
- `HQCP#0#0` 表示连接唯一ID，可用于上下文追踪。
- `+)4` 表示连接池新增了一条连接，当前连接总数为 4
- `-)3` 表示连接池销毁了一条连接，当前连接总数为 3
- 连接池 `HQCP#0` 的 `+|-)` 日志输出依赖于 `verbose` 打开

#### 7.1.3 连接检出与回收
`verbose` 或 `printSql` 打开时，连接被检出 `DataSource.getConnection()` 和回收 `conn.close()` 会记录相关信息（配合日志格式的线程名，可以定位连接泄露问题等）。<br/>
示例如下：
```
[BusinessThread-0 ] [DEBUG] HQCP#1#0.getConnection(true), use 152 ns

其他日志...

[BusinessThread-0 ] [DEBUG] HQCP#1#0.close()[false] use 152 ns
```
其中： 
- `getConnection(true)` 的 `true` 表示返回连接的 `autoCommit` 属性为 `true`。
- `close()[false]` 的 `false` 表示连接没有真实关闭（仅回收到连接池），否则表示连接真实断开与数据库服务器的连接。

#### 7.1.4 `Connection` 对象方法的调用 
**当 `verbose` 打开时**，`Connection` 对象方法的调用会输出不同级别的日志。示例如下：
```
[BusinessThread-0 ] [DEBUG] HQCP#0#0.setAutoCommit(true) use 491,583 ns
[BusinessThread-0 ] [INFO ] HQCP#0#0 * createStatement()[1], use 25,453,708 ns
...
[BusinessThread-0 ] [INFO ] HQCP#0#0 * prepareStatement(select * from db1.table1 where id=?)[1], use 64,320,583 ns
...
[BusinessThread-0 ] [DEBUG] HQCP#0#0.commit() use 38,084 ns
...
[BusinessThread-0 ] [DEBUG] HQCP#0#0.rollback() use 87,750 ns
```
- hq-cp 连接池缓存了 `Statement` `PreparedStatement` `CallableStatement`，因此有新的对象创建时，会输出 `INFO` 级别以 `*` 开头的日志；否则直接返回缓存对象的话，输出 `TRACE` 级别的日志（通常不会输出）。
- `* createStatement()[1]` 中的 `[1]` 表示当前连接创建了1个 `Statement` 对象，`* prepareStatement(...)` `* prepareCall(...)` 的日志同理。
- 其他方法调用默认是 `DEBUG` 级别日志。
          
#### 7.1.5 连接池状态信息
以下场景，hq-cp 连接池会打印连接池的状态信息，以供问题排查或配置优化：
- 连接池监控线程定时
- 当连接耗尽，业务线程获取连接失败时

示例如下：
```
1: [DEBUG] HQCP#0#0 checkout by BusinessThread-0 for 5 ms[IDLE]
2: [INFO ] HQCP#0#1.STMT#3 invoking execute(SELECT * FROM demo) use 25,453,708 ns
3: [INFO ] HQCP#0#1 checkout by BusinessThread-1 for 30 ms[BUSYING]
4: [WARN ] HQCP#0#2.STMT#2 invoking executeUpdate(UPDATE demo SET name='zhangsan' WHERE id=123) use 125,453,708 ns
5: [WARN ] HQCP#0#2 checkout by BusinessThread-2 for 150 ms[BUSYING]
6: [INFO ] HQCP#0: checkout:3/connected:5/max:10
```
##### 状态日志的详细说明：
- 活跃（被检出的）连接状态<br/>
第1行日志显示：连接 `HQCP#0#0` 被线程 `BusinessThread-0` 检出了 `5 ms`，处于空闲 `IDLE` 状态，没有在执行SQL语句 <br/>
第2-3行日志显示：连接 `HQCP#0#1` 被线程 `BusinessThread-1` 检出了 `30 ms`，并且当前其 `STMT#3` 正在执行SQL `SELECT * FROM demo`，持续了 `25 ms` <br/>
第4-5行日志显示：连接 `HQCP#0#2` 被线程 `BusinessThread-2` 检出了 `150 ms`，并且当前其 `STMT#2` 正在执行SQL `UPDATE demo SET name='zhangsan' WHERE id=123`，持续了 `125 ms`
- 当前连接数量信息<br/>
第6行日志：连接池 `HQCP#0` (`checkout`)有3条连接被检出，(`connected`)总共建立了5条连接，(`max`)最多允许10条连接

##### 日志级别说明：
- <u>活跃连接状态</u>的日志级别取决于连接被检出或者语句执行的耗时，即 当耗时大于 `warnSqlThreshold` 时，打印 `WARN` 日志，否则 大于 `infoSqlThreshold` 时，打印 `INFO` 日志，再否则 打印 `DEBUG` 日志
- 当 `verbose` 打开 或 连接耗尽 时，<u>连接数量信息</u>打印 `INFO` 日志，否则打印 `DEBUG` 日志

#### 7.1.6 其他常见告警信息
- 当连接耗尽，需要等待 `checkoutTimeoutMillisec` 所配置的时间时，会先输出一条 `INFO` 告警日志，提示连接吃紧：
```
[INFO] connections of HQCP#0 to jdbc:oracle:thin:@localhost:1521:orcl exhausted, wait 10000 ms for idle connection
```
- 如果等待 `checkoutTimeoutMillisec` 后还没有可用连接，则会抛出 `SQLException`，异常的message类似：`Timeout on waiting for an available connection of HQCP#0 to jdbc:mysql:url`


### 7.2 SQL执行日志
**当 `printSql` 打开时**，连接池会打印SQL语句执行的情况，示例：
```
[INFO ] HQCP#0#0.STMT#0.executeQuery(SELECT * FROM demo)[rs=#0] use 47,284,084 ns
```
当SQL耗时大于 `warnSqlThreshold` 时，打印 `WARN` 日志，否则 大于 `infoSqlThreshold` 时，打印 `INFO` 日志，再否则 打印 `DEBUG` 日志

---

## 总结

整体来说，hq-cp 模块在高并发场景下具有较好的稳定性，同时通过监控与调优功能，为开发者提供了灵活的数据库连接解决方案，是 Java 应用中一个值得考虑的选择。

更多使用细节及配置说明，请参考相关源码注释。

---

*Generated by [🤖 OpenAI O3 Mini]*