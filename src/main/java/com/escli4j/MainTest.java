package com.escli4j;

import java.io.IOException;
import java.net.InetAddress;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.Mapping;
import com.escli4j.model.EsEntity;

public class MainTest {

    public static void main(String[] args) throws IOException {
        new Mapping(new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("es"), 9200))).migrate();
    }

    @Type(index = "testIndex", type = "testType")
    public static class Test1 extends EsEntity {

        @Field(datatype = Datatype.BOOLEAN)
        public String some;
        @Field(datatype = Datatype.OBJECT)
        public Test2 test2;

    }

    public static class Test2 extends EsEntity {

        @Field(datatype = Datatype.TEXT)
        public String some2;

    }

}
