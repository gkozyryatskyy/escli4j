package com.escli4j;

import java.io.IOException;
import java.net.InetAddress;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.escli4j.annotations.Type;
import com.escli4j.dao.ChildEntityDao;
import com.escli4j.model.EsChildEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class MainTest {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        try {
            Client esClient = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
            System.out.println("started");
            // Mapping mapping = new Mapping(esClient);
            // mapping.migrate();
            TestDao dao = new TestDao(esClient);
            System.out.println(dao.get("AV80Qyj3wN7gDfKCr20h", "qqwerqwerqwerwqer"));
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Type(index = "tt", type = "clip", parent = "video", update = true)
    public static class TestObject extends EsChildEntity {

    }

    public static class TestDao extends ChildEntityDao<TestObject> {

        public TestDao(Client client) {
            super(TestObject.class, client);
        }

    }

}
