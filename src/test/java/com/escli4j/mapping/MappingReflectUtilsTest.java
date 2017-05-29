package com.escli4j.mapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.escli4j.annotations.Type;
import com.escli4j.mapping.model.ParrentMappingModel1;
import com.escli4j.mapping.model.SubMappingModel2;
import com.escli4j.mapping.model.TestMappingModel1;
import com.escli4j.mapping.model.TestMappingModel2;
import com.escli4j.mapping.model.TestMappingModel3;
import com.escli4j.mapping.model.TestMappingModel4;
import com.escli4j.model.TestModel1;

public class MappingReflectUtilsTest {

    @Test
    public void getAnnotatedClassesAllTest() throws IOException {
        Set<Class<?>> classes = MappingReflectUtils.getAnnotatedClasses(null, Type.class);
        Assert.assertTrue(classes.remove(TestModel1.class));
        Assert.assertTrue(classes.remove(TestMappingModel1.class));
        Assert.assertTrue(classes.remove(TestMappingModel2.class));
        Assert.assertTrue(classes.remove(TestMappingModel3.class));
        Assert.assertTrue(classes.remove(TestMappingModel4.class));
        Assert.assertTrue(classes.remove(ParrentMappingModel1.class));
        Assert.assertTrue(classes.remove(SubMappingModel2.class));
        Assert.assertEquals(classes.toString(), 0, classes.size());
    }

    @Test
    public void getAnnotatedClassesParentPackageTest() {
        Set<Class<?>> classes = MappingReflectUtils.getAnnotatedClasses("com.escli4j", Type.class);
        Assert.assertTrue(classes.remove(TestModel1.class));
        Assert.assertTrue(classes.remove(TestMappingModel1.class));
        Assert.assertTrue(classes.remove(TestMappingModel2.class));
        Assert.assertTrue(classes.remove(TestMappingModel3.class));
        Assert.assertTrue(classes.remove(TestMappingModel4.class));
        Assert.assertTrue(classes.remove(ParrentMappingModel1.class));
        Assert.assertTrue(classes.remove(SubMappingModel2.class));
        Assert.assertEquals(classes.toString(), 0, classes.size());
    }

    @Test
    public void getAnnotatedClassesDifferentPackageTest() {
        Set<Class<?>> classes = MappingReflectUtils.getAnnotatedClasses("com.escli4j.model", Type.class);
        Assert.assertTrue(classes.remove(TestModel1.class));
        Assert.assertEquals(classes.toString(), 0, classes.size());
    }

    @Test
    public void getAllAnnotatedFieldsTest() {
        List<Field> fields = MappingReflectUtils.getAllAnnotatedFields(SubMappingModel2.class,
                com.escli4j.annotations.Field.class);
        Assert.assertEquals(6, fields.size());
    }

}
