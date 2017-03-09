package com.escli4j.mapping;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.escli4j.mapping.model.SubMappingModel2;
import com.escli4j.mapping.model.TestMappingModel1;
import com.escli4j.mapping.model.TestMappingModel2;
import com.escli4j.mapping.model.TestMappingModel3;

public class MappingUtilsTest {

    public static String readJson(String pathToJson) {
        try {
            return new String(Files.readAllBytes(
                    Paths.get(Thread.currentThread().getContextClassLoader().getResource(pathToJson).toURI())));

        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    public void getMappingBuilderTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test1", TestMappingModel1.class);
        Assert.assertEquals(readJson("TestMappingModel1.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderWithParentTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test_sub2", SubMappingModel2.class);
        Assert.assertEquals(readJson("SubMappingModel2.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderNestedArrayTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test2", TestMappingModel2.class);
        Assert.assertEquals(readJson("TestMappingModel2.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderChildTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test_child3", "test1", TestMappingModel3.class);
        Assert.assertEquals(readJson("TestMappingModel3.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }
    
    @Test
    public void getEmptySettingsBuilderTest() throws IOException {
        String builder = MappingUtils.getSettingsBuilder(Arrays.asList(TestMappingModel2.class.getAnnotations()));
        Assert.assertNull(builder);
    }

    @Test
    public void getSettingsBuilderEdgeNgramTest() throws IOException {
        String builder = MappingUtils.getSettingsBuilder(Arrays.asList(TestMappingModel1.class.getAnnotations()));
        System.out.println(builder);
        Assert.assertEquals(readJson("TestMappingModel1Settings.json").replaceAll("\\s+|\\n+|\\t+", ""), builder);
    }

}
