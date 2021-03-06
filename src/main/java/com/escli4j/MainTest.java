package com.escli4j;

import java.io.IOException;
import java.net.InetAddress;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.escli4j.mapping.Mapping;

public class MainTest {

    @SuppressWarnings({ "resource", "unused" })
    public static void main(String[] args) throws IOException {
        try {
            Client esClient = new PreBuiltTransportClient(Settings.EMPTY)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
            System.out.println("started");
            Mapping mapping = new Mapping(esClient);
            // mapping.migrate();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
