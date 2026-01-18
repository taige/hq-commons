package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import io.hqwu.commons.util.StringUtil;
import org.apache.ibatis.type.JdbcType;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * mybatis plus 提供的代码生成器
 * 可以快速生成 Entity、Mapper、Mapper XML、Service、Controller 等各个模块的代码
 *
 * @link <a href="https://baomidou.com/guides/new-code-generator/">代码生成器</a>
 */
public class CodeGenerator {

    private final String basePackage;

    private final String moduleName;

    private final DataSource dataSource;

    private String author = "Mr. NoName";

    /**
     * 文件生成的基础目录 <br/>
     * 默认值：user.dir 环境变量
     */
    private String baseOutputDir;

    /**
     * java 文件生成路径，相对 {@link #baseOutputDir} 的路径
     */
    private String javaOutputPath = "src/main/java";

    /**
     * mapper.xml 文件生成路径，相对 {@link #baseOutputDir} 的路径
     */
    private String mapperXmlOutputPath = "src/main/resources/mapper/";

    /**
     * 文件模板目录
     */
    private String templatesPath = "generator/templates/";

    /**
     * xml 文件模板
     */
    private String mapperXmlTemplatePath = "mapper.xml";
    /**
     * mapper 文件模板
     */
    private String mapperTemplatePath = "mapper.java";
    /**
     * entity 文件模板
     */
    private String entityTemplatePath = "entity.java";
    /**
     * service 文件模板
     */
    private String serviceTemplatePath = "service.java";
    /**
     * serviceImpl 文件模板
     */
    private String serviceImplTemplatePath = "serviceImpl.java";

    /**
     * controller 文件模板
     */
    private String controllerTemplatePath = "controller.java";

    /**
     * 实体类名格式，默认为 %sPO，即 以 PO 结尾
     */
    private String entityNameFormat = "%sPO";

    /**
     * 生成service相关类，默认：false - 不生成
     */
    private boolean serviceEnabled = false;

    /**
     * 生成controller类，默认：false - 不生成
     */
    private boolean controllerEnabled = false;

    /**
     * 参考 {@link IdType}
     */
    private IdType idType;

    public CodeGenerator(String basePackage, String moduleName, DataSource dataSource) {
        this.basePackage = basePackage;
        this.moduleName = moduleName;
        this.dataSource = dataSource;
    }

    /**
     * generate code for tables (not override exist file & try parse name prefix)
     * @param tables table names to generate code
     */
    public void generate(String... tables) {
        generate(false, tables);
    }

    /**
     * generate code for tables (try parse name prefix)
     * @param fileOverride whether override file if exist
     * @param tables       table names to generate code
     */
    public void generate(boolean fileOverride, String... tables) {
        generate(fileOverride, true, tables);
    }

    /**
     * generate code for tables
     * @param fileOverride whether override file if exist
     * @param hasPrefix    whether table name has prefix
     * @param tables       table names to generate code
     */
    public void generate(boolean fileOverride, boolean hasPrefix, String... tables) {
        String[] prefix = hasPrefix ? Arrays.stream(tables).map(t -> {
            String[] buf = t.split("_", 2);
            if (buf.length > 1) {
                return buf[0];
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet()).toArray(new String[]{}) : new String[]{};
        FastAutoGenerator generator = buildGenerator();
        if (fileOverride) {
            generator.strategyConfig(builder ->
                    builder.controllerBuilder().enableFileOverride()
                            .serviceBuilder().enableFileOverride()
                            .mapperBuilder().enableFileOverride()
                            .entityBuilder().enableFileOverride());
        }
        if (! serviceEnabled) {
            generator.strategyConfig(builder ->
                    builder.serviceBuilder().disable());
        }
        if (! controllerEnabled) {
            generator.strategyConfig(builder ->
                    builder.controllerBuilder().disable());
        }
        if (StringUtil.isNotBlank(author)) {
            generator.globalConfig(builder ->
                    builder.author(author));
        }
        if (idType != null) {
            generator.strategyConfig(builder ->
                    builder.entityBuilder().idType(idType)
            );
        }
        generator.strategyConfig(builder ->
                builder.addInclude(tables)
                        .addTablePrefix(prefix))
                .execute();
    }

    protected FastAutoGenerator buildGenerator() {
        String projectPath = StringUtil.isBlank(baseOutputDir) ? System.getProperty("user.dir") : baseOutputDir;
        return FastAutoGenerator.create(new DataSourceConfig.Builder(dataSource))
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            // 兼容旧版本, 长度 >1 的 TINYINT 转换成Integer
                            if (JdbcType.TINYINT == metaInfo.getJdbcType()) {
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        }))
                .packageConfig(builder ->
                        builder.parent(basePackage)
                                .moduleName(moduleName)
                                .pathInfo(Collections.singletonMap(OutputFile.xml, new File(projectPath, mapperXmlOutputPath).getPath())))
                .strategyConfig(builder ->
                        builder.entityBuilder()
                                .formatFileName(entityNameFormat)
                                .naming(NamingStrategy.underline_to_camel)
                                .columnNaming(NamingStrategy.underline_to_camel)
                                .enableLombok()
                                .enableChainModel()
                                .disableSerialVersionUID()
                                .javaTemplate(new File(templatesPath, entityTemplatePath).getPath())
                                .mapperBuilder()
                                .mapperTemplate(new File(templatesPath, mapperTemplatePath).getPath())
                                .mapperXmlTemplate(new File(templatesPath, mapperXmlTemplatePath).getPath())
                                .serviceBuilder()
                                .serviceTemplate(new File(templatesPath, serviceTemplatePath).getPath())
                                .serviceImplTemplate(new File(templatesPath, serviceImplTemplatePath).getPath())
                                .controllerBuilder()
                                .enableRestStyle()
                                .enableHyphenStyle()
                                .template(new File(templatesPath, controllerTemplatePath).getPath())) // 不生成 controller
                .globalConfig(builder ->
                        builder.outputDir(new File(projectPath, javaOutputPath).getPath())
                                .disableOpenDir()

                )
                .templateEngine(new FreemarkerTemplateEngine() {
                    @Override
                    protected boolean isCreate(File file, boolean fileOverride) {
                        boolean b = super.isCreate(file, fileOverride);
                        if (! b) {
                            System.err.printf("文件[%s]已存在，且未开启文件覆盖配置，需要开启配置可到策略配置中设置！！！%n", file.getName());
                        }
                        return b;
                    }
                });
    }

    public CodeGenerator baseOutputDir(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
        return this;
    }

    public CodeGenerator author(String author) {
        this.author = author;
        return this;
    }

    public CodeGenerator idType(IdType idType) {
        this.idType = idType;
        return this;
    }

    public CodeGenerator controllerTemplatePath(String controllerTemplatePath) {
        if (StringUtil.isNotBlank(controllerTemplatePath)) {
            this.controllerTemplatePath = controllerTemplatePath;
        }
        return this;
    }

    public CodeGenerator mapperXmlOutputPath(String mapperXmlOutputPath) {
        if (StringUtil.isNotBlank(mapperXmlOutputPath)) {
            this.mapperXmlOutputPath = mapperXmlOutputPath;
        }
        return this;
    }

    public CodeGenerator javaOutputPath(String javaOutputPath) {
        if (StringUtil.isNotBlank(javaOutputPath)) {
            this.javaOutputPath = javaOutputPath;
        }
        return this;
    }

    public CodeGenerator templatesPath(String templatesPath) {
        if (StringUtil.isNotBlank(templatesPath)) {
            this.templatesPath = templatesPath;
        }
        return this;
    }

    public CodeGenerator mapperXmlTemplatePath(String mapperXmlTemplatePath) {
        if (StringUtil.isNotBlank(mapperXmlTemplatePath)) {
            this.mapperXmlTemplatePath = mapperXmlTemplatePath;
        }
        return this;
    }

    public CodeGenerator mapperTemplatePath(String mapperTemplatePath) {
        if (StringUtil.isNotBlank(mapperTemplatePath)) {
            this.mapperTemplatePath = mapperTemplatePath;
        }
        return this;
    }

    public CodeGenerator entityTemplatePath(String entityTemplatePath) {
        if (StringUtil.isNotBlank(entityTemplatePath)) {
            this.entityTemplatePath = entityTemplatePath;
        }
        return this;
    }

    public CodeGenerator serviceTemplatePath(String serviceTemplatePath) {
        if (StringUtil.isNotBlank(serviceTemplatePath)) {
            this.serviceTemplatePath = serviceTemplatePath;
        }
        return this;
    }

    public CodeGenerator serviceImplTemplatePath(String serviceImplTemplatePath) {
        if (StringUtil.isNotBlank(serviceImplTemplatePath)) {
            this.serviceImplTemplatePath = serviceImplTemplatePath;
        }
        return this;
    }

    public CodeGenerator entityNameFormat(String entityNameFormat) {
        if (StringUtil.isNotBlank(entityNameFormat)) {
            this.entityNameFormat = entityNameFormat;
        }
        return this;
    }

    public CodeGenerator enableService() {
        this.serviceEnabled = true;
        return this;
    }

    public CodeGenerator enableController() {
        this.controllerEnabled = true;
        return this;
    }
}
