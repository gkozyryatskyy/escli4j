package com.escli4j.mapping;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        XContentBuilder builder = MappingUtils.getMappingBuilder("test", TestMappingModel1.class);
        Assert.assertEquals(readJson("TestMappingModel1.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderWithParentTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test", SubMappingModel2.class);
        Assert.assertEquals(readJson("SubMappingModel2.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderNestedArrayTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("test", TestMappingModel2.class);
        Assert.assertEquals(readJson("TestMappingModel2.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

    @Test
    public void getMappingBuilderChildTest() throws IOException {
        XContentBuilder builder = MappingUtils.getMappingBuilder("testChild", "test", TestMappingModel3.class);
        System.out.println(builder.string());
        Assert.assertEquals(readJson("TestMappingModel3.json").replaceAll("\\s+|\\n+|\\t+", ""), builder.string());
    }

}
