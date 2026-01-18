package io.hqwu.commons.mybatisplus;

import com.baomidou.mybatisplus.annotation.IdType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class CodeGeneratorTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testGenerateDefault() throws IOException {
        String baseOutputDir = "target/test-generated-default";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        // Minimal configuration: only required constructor args
        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "def", dataSource);

        // Set output dir to avoid polluting source tree
        generator.baseOutputDir(baseOutputDir);
        
        // Enable Service and Controller to verify they can be generated with default templates
        generator.enableService().enableController();

        // Use default generate method
        generator.generate("gpf_areas");

        String basePath = baseOutputDir + "/src/main/java/io/hqwu/commons/mybatisplus/testgen/def/";

        // Verify Entity (Default format %PO)
        File entityFile = new File(basePath + "entity/AreasPO.java");
        assertTrue(entityFile.exists(), "Entity file should exist");
        String entityContent = new String(Files.readAllBytes(entityFile.toPath()));
        assertTrue(entityContent.contains("class AreasPO"), "Class name should be AreasPO");
        assertTrue(entityContent.contains("@TableName(\"GPF_AREAS\")"), "Should contain TableName annotation");
        assertTrue(entityContent.contains("private String id;"), "Should contain id field");
        assertTrue(entityContent.contains("private String name;"), "Should contain name field");
        assertTrue(entityContent.contains("private Integer parentId;"), "Should contain parentId field");
        // Default author is "Mr. NoName"
        assertTrue(entityContent.contains("@author Mr. NoName"), "Should contain default author");

        // Verify Mapper
        File mapperFile = new File(basePath + "mapper/AreasMapper.java");
        assertTrue(mapperFile.exists(), "Mapper file should exist");
        String mapperContent = new String(Files.readAllBytes(mapperFile.toPath()));
        assertTrue(mapperContent.contains("public interface AreasMapper extends BaseMapper<AreasPO>"), "Mapper should extend BaseMapper");

        // Verify Mapper XML
        // mapperXmlOutputPath is relative to baseOutputDir, default is "src/main/resources/mapper/"
        File mapperXmlFile = new File(baseOutputDir + "/src/main/resources/mapper/AreasMapper.xml");
        
        assertTrue(mapperXmlFile.exists(), "Mapper XML file should exist at " + mapperXmlFile.getAbsolutePath());

        // Verify Service and Controller are generated (enabled)
        File serviceFile = new File(basePath + "service/IAreasService.java");
        assertTrue(serviceFile.exists(), "Service file should exist");
        String serviceContent = new String(Files.readAllBytes(serviceFile.toPath()));
        assertTrue(serviceContent.contains("public interface IAreasService extends IService<AreasPO>"), "Service should extend IService");

        File serviceImplFile = new File(basePath + "service/impl/AreasServiceImpl.java");
        assertTrue(serviceImplFile.exists(), "ServiceImpl file should exist");
        String serviceImplContent = new String(Files.readAllBytes(serviceImplFile.toPath()));
        assertTrue(serviceImplContent.contains("public class AreasServiceImpl extends ServiceImpl<AreasMapper, AreasPO> implements IAreasService"), "ServiceImpl should extend ServiceImpl and implement IAreasService");
        assertTrue(serviceImplContent.contains("@Service"), "ServiceImpl should have @Service annotation");

        File controllerFile = new File(basePath + "controller/AreasController.java");
        assertTrue(controllerFile.exists(), "Controller file should exist");
        String controllerContent = new String(Files.readAllBytes(controllerFile.toPath()));
        assertTrue(controllerContent.contains("@RestController"), "Controller should have @RestController annotation (default REST style)");
        assertTrue(controllerContent.contains("@RequestMapping(\"/def/areas-po\")"), "Controller should have @RequestMapping with module name and entity path");
        assertTrue(controllerContent.contains("public class AreasController"), "Controller class name should be AreasController");

        // Test repeat generation without override (should trigger warning log, but not fail)
        generator.generate("gpf_areas");
        assertTrue(entityFile.exists(), "Entity file should still exist after repeat generation");
    }

    @Test
    public void testGenerateCustomConfig() throws IOException {
        String baseOutputDir = "target/test-generated-custom";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "custom", dataSource);

        generator.baseOutputDir(baseOutputDir)
                .author("Test Author")
                .idType(IdType.AUTO)
                .entityNameFormat("%sEntity");

        // Do not enable service and controller (default false)

        // Generate with fileOverride=true, hasPrefix=false
        // This covers the branch where prefix is NOT parsed, so "gpf_areas" becomes "GpfAreas" instead of "Areas"
        generator.generate(true, false, "gpf_areas");

        String basePath = baseOutputDir + "/src/main/java/io/hqwu/commons/mybatisplus/testgen/custom/";

        // Verify Entity (Format %sEntity, No prefix removal -> GpfAreasEntity)
        File entityFile = new File(basePath + "entity/GpfAreasEntity.java");
        assertTrue(entityFile.exists(), "Entity file should exist: " + entityFile.getAbsolutePath());
        String entityContent = new String(Files.readAllBytes(entityFile.toPath()));
        assertTrue(entityContent.contains("class GpfAreasEntity"), "Class name should be GpfAreasEntity");
        assertTrue(entityContent.contains("@TableName(\"GPF_AREAS\")"), "Should contain TableName annotation");
        assertTrue(entityContent.contains("@author Test Author"), "Should contain custom author");
        assertTrue(entityContent.contains("type = IdType.AUTO"), "Should contain IdType.AUTO");

        // Verify Service and Controller do NOT exist
        File serviceFile = new File(basePath + "service/IGpfAreasService.java");
        assertFalse(serviceFile.exists(), "Service file should NOT exist");

        File controllerFile = new File(basePath + "controller/GpfAreasController.java");
        assertFalse(controllerFile.exists(), "Controller file should NOT exist");

        // Verify Mapper exists
        File mapperFile = new File(basePath + "mapper/GpfAreasMapper.java");
        assertTrue(mapperFile.exists(), "Mapper file should exist");
    }

    @Test
    public void testGeneratePathConfig() throws IOException {
        String baseOutputDir = "target/test-generated-paths";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "paths", dataSource);

        generator.baseOutputDir(baseOutputDir)
                .javaOutputPath("src/main/generated-java")
                .mapperXmlOutputPath("src/main/resources/generated-mapper");

        // Test generate(boolean fileOverride, String... tables)
        // fileOverride = true, implies hasPrefix = true (default in this overload)
        generator.generate(true, "gpf_areas");

        String basePath = baseOutputDir + "/src/main/generated-java/io/hqwu/commons/mybatisplus/testgen/paths/";

        // Verify Entity exists in new path
        // hasPrefix=true, so "gpf_areas" -> "AreasPO" (default format)
        File entityFile = new File(basePath + "entity/AreasPO.java");
        assertTrue(entityFile.exists(), "Entity file should exist in custom java output path: " + entityFile.getAbsolutePath());

        // Verify Mapper XML exists in new path
        File mapperXmlFile = new File(baseOutputDir + "/src/main/resources/generated-mapper/AreasMapper.xml");
        assertTrue(mapperXmlFile.exists(), "Mapper XML file should exist in custom xml output path: " + mapperXmlFile.getAbsolutePath());
    }

    @Test
    public void testGenerateWithCustomTemplates() throws IOException {
        String baseOutputDir = "target/test-generated-templates";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "tpl", dataSource);
        generator.baseOutputDir(baseOutputDir);
        
        // Set custom template paths (using default values for now, but exercising the setters)
        generator.templatesPath("generator/templates/")
                 .entityTemplatePath("entity.java")
                 .mapperTemplatePath("mapper.java")
                 .mapperXmlTemplatePath("mapper.xml")
                 .serviceTemplatePath("service.java")
                 .serviceImplTemplatePath("serviceImpl.java")
                 .controllerTemplatePath("controller.java");
                 
        generator.generate("gpf_areas");
        
        File entityFile = new File(baseOutputDir + "/src/main/java/io/hqwu/commons/mybatisplus/testgen/tpl/entity/AreasPO.java");
        assertTrue(entityFile.exists(), "Entity file should exist when using template setters");
    }

    @Test
    public void testGenerateWithMixedTables() throws IOException {
        // This test ensures that the prefix parsing logic handles tables without underscores gracefully
        // and still generates code for valid tables.
        String baseOutputDir = "target/test-generated-mixed";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "mixed", dataSource);
        generator.baseOutputDir(baseOutputDir);
        
        // hasPrefix = true. "gpf_areas" -> "gpf". "SimpleTable" -> null.
        // Prefix set to ["gpf"].
        generator.generate(false, true, "gpf_areas", "SimpleTable");
        
        // Verify gpf_areas generated correctly (prefix stripped)
        File entityFile = new File(baseOutputDir + "/src/main/java/io/hqwu/commons/mybatisplus/testgen/mixed/entity/AreasPO.java");
        assertTrue(entityFile.exists(), "Entity file for gpf_areas should exist");
    }

    @Test
    public void testTinyIntConversion() throws IOException {
        String baseOutputDir = "target/test-generated-tinyint";
        FileSystemUtils.deleteRecursively(new File(baseOutputDir));

        CodeGenerator generator = new CodeGenerator("io.hqwu.commons.mybatisplus.testgen", "tinyint", dataSource);
        generator.baseOutputDir(baseOutputDir);

        // Generate code for tinyint_test table
        // Disable prefix parsing (hasPrefix=false) so "tinyint_test" generates "TinyintTestPO" instead of "TestPO"
        generator.generate(false, false, "tinyint_test");

        String basePath = baseOutputDir + "/src/main/java/io/hqwu/commons/mybatisplus/testgen/tinyint/";
        File entityFile = new File(basePath + "entity/TinyintTestPO.java");
        assertTrue(entityFile.exists(), "Entity file should exist");

        String entityContent = new String(Files.readAllBytes(entityFile.toPath()));
        
        // Verify that 'status' (TINYINT(2)) is converted to Integer
        assertTrue(entityContent.contains("private Integer status;"), "TINYINT(2) should be converted to Integer");
        
        // Verify that 'flag' (TINYINT(1)) is converted to Integer (based on current logic which converts all TINYINT to Integer)
        assertTrue(entityContent.contains("private Integer flag;"), "TINYINT(1) should be converted to Integer based on current CodeGenerator logic");
    }
}
