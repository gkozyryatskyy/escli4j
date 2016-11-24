package com.escli4j;

import java.io.IOException;
import java.net.InetAddress;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.escli4j.annotations.Field;
import com.escli4j.annotations.InnerField;
import com.escli4j.annotations.Type;
import com.escli4j.mapping.DataType;
import com.escli4j.mapping.Mapping;
import com.escli4j.model.EsEntity;

public class Main {

    // TODO move to tests

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        new Mapping(new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300)))
                        .migrate();
    }

    @Type(index = "test", type = "test")
    public class Video extends EsEntity {

        @Field(dataType = DataType.TEXT)
        public String url;
        @Field(dataType = DataType.OBJECT)
        public Inner inner;
        @Field(dataType = DataType.KEYWORD, fields = { @InnerField(dataType = DataType.COMPLETION, name = "suggest") })
        public String[] tags;

    }

    public class Inner {
        @Field(dataType = DataType.TEXT)
        public String innerText;
    }

}
