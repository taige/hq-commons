package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.rules.FileType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.umpay.commons.util.Logger;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * mybatis plus 提供的代码生成器
 * 可以快速生成 Entity、Mapper、Mapper XML、Service、Controller 等各个模块的代码
 *
 * @link https://mp.baomidou.com/guide/generator.html
 */
public class CodeGenerator {
    private static final Logger LOGGER = new Logger();

    // java文件路径
    private static final String JAVA_PACKAGE_URL = "/src/main/java";

    private final String basePackge;

    private final String moduleName;

    private final DataSource dataSource;

    private final DbType dbType;

    private String author = "Mr. NoName";

    // mapper.xml 文件生成路径
    private String mapperXmlOutputPath = "/src/main/resources/mapper/";

    // 文件模板目录
    private String templatesPath = "generator/templates/";

    // xml 文件模板
    private String mapperXmlTemplatePath = "mapper.xml";
    // mapper 文件模板
    private String mapperTemplatePath = "mapper.java";
    // entity 文件模板
    private String entityTemplatePath = "entity.java";
    // service 文件模板
    private String serviceTemplatePath = "service.java";
    // serviceImpl 文件模板
    private String serviceImplTemplatePath = "serviceImpl.java";
    // controller 文件模板
    private String controllerTemplatePath = "controller.java";

    private String baseOutputDir;

    private IdType idType;

    public CodeGenerator(String basePackge, String moduleName, DataSource dataSource, DbType dbType) {
        this.basePackge = basePackge;
        this.moduleName = moduleName;
        this.dataSource = dataSource;
        this.dbType = dbType;
    }

    /**
     * generate code for tables (not override exist file & try parse name prefix)
     * @param table table names to generate code
     */
    public void generate(String... table) {
        generate(false, table);
    }

    /**
     * generate code for tables (try parse name prefix)
     * @param fileOverride whether override file if exist
     * @param table        table names to generate code
     */
    public void generate(boolean fileOverride, String... table) {
        generate(fileOverride, true, table);
    }

    /**
     * generate code for tables
     * @param fileOverride whether override file if exist
     * @param hasPrefix    whether table name has prefix
     * @param table        table names to generate code
     */
    public void generate(boolean fileOverride, boolean hasPrefix, String... table) {
        String[] prefix = hasPrefix ? Arrays.stream(table).map(t->{
            String[] buf = t.split("_", 2);
            if (buf.length > 1) {
                return buf[0];
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet()).toArray(new String[] {}) : new String[] {};
        String projectPath = baseOutputDir == null ? System.getProperty("user.dir") : baseOutputDir;
        ConfigBuilder config = new ConfigBuilder(
                new PackageConfig()
                        .setParent(basePackge)
                        .setModuleName(moduleName),
                new DataSourceConfig() {
                    @Override
                    public DbType getDbType() {
                        return dbType;
                    }

                    @Override
                    public Connection getConn() {
                        try {
                            return dataSource.getConnection();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                new StrategyConfig()
                        .setNaming(NamingStrategy.underline_to_camel)
                        .setColumnNaming(NamingStrategy.underline_to_camel)
                        .setEntityLombokModel(true)
                        .setRestControllerStyle(true)
                        .setInclude(table)
                        .setControllerMappingHyphenStyle(true)
                        .setEntitySerialVersionUID(false)
                        .setChainModel(true)
                        .setTablePrefix(prefix),
                new TemplateConfig()
                        .setXml(new File(templatesPath, mapperXmlTemplatePath).getPath())
                        .setMapper(new File(templatesPath, mapperTemplatePath).getPath())
                        .setEntity(new File(templatesPath, entityTemplatePath).getPath())
                        .setService(new File(templatesPath, serviceTemplatePath).getPath())
                        .setServiceImpl(new File(templatesPath, serviceImplTemplatePath).getPath())
                        .setController(controllerTemplatePath == null ? null : new File(templatesPath, controllerTemplatePath).getPath()),
                new GlobalConfig()
                        .setOutputDir(new File(projectPath, JAVA_PACKAGE_URL).getPath())
                        .setAuthor(author)
                        .setOpen(false)
                        .setFileOverride(fileOverride)
                        .setServiceName("I%sService")
                        .setEntityName("T%s")
                        .setIdType(idType)
        );
        ;
        config.getPathInfo().put(ConstVal.XML_PATH,
                new File(new File(projectPath, mapperXmlOutputPath), moduleName).getPath());
        AutoGenerator generator = new AutoGenerator();
        generator.setConfig(config);
        generator.setTemplateEngine(new FreemarkerTemplateEngine() {
            @Override
            protected boolean isCreate(FileType fileType, String filePath) {
                boolean b = super.isCreate(fileType, filePath);
                if (! b) {
                    LOGGER.info("file {} EXIST, and will NOT override!", filePath);
                }
                return b;
            }
        });
        generator.execute();
    }

    public void setBaseOutputDir(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setControllerTemplatePath(String controllerTemplatePath) {
        this.controllerTemplatePath = controllerTemplatePath;
    }

    public void setIdType(IdType idType) {
        this.idType = idType;
    }

}
