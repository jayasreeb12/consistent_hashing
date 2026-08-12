package com.ssn.consistent_hashing.service;

import com.mongodb.client.MongoClients;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MongoNodeManager {
    private final Map<String, MongoTemplate> nodeTemplates = new ConcurrentHashMap<>();

    public void registerNode(String nodeName, String connectionUri, String databaseName) {
        MongoTemplate template = new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(MongoClients.create(connectionUri), databaseName)
        );
        nodeTemplates.put(nodeName, template);
    }

    public void unregisterNode(String nodeName) {
        nodeTemplates.remove(nodeName);
    }

    public MongoTemplate getTemplate(String nodeName) {
        MongoTemplate template = nodeTemplates.get(nodeName);
        if (template == null) {
            throw new IllegalArgumentException("Node not registered: " + nodeName);
        }
        return template;
    }

    public Map<String, MongoTemplate> getAllTemplates() {
        return nodeTemplates;
    }
}