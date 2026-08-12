package com.ssn.consistent_hashing.service;

import com.ssn.consistent_hashing.algo.ConsistentHashRing;
import com.ssn.consistent_hashing.model.Student;
import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StudentRoutingService {

    private final MongoNodeManager nodeManager;
    private ConsistentHashRing<String> hashRing;
    private final int VIRTUAL_NODES = 100;

    public StudentRoutingService(MongoNodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @PostConstruct
    public void init() {
        // Registers initial 3 nodes running on Docker ports 27017, 27018, and 27019
        nodeManager.registerNode("mongo-1", "mongodb://localhost:27017", "student_db");
        nodeManager.registerNode("mongo-2", "mongodb://localhost:27018", "student_db");
        nodeManager.registerNode("mongo-3", "mongodb://localhost:27019", "student_db");

        List<String> initialNodes = Arrays.asList("mongo-1", "mongo-2", "mongo-3");
        hashRing = new ConsistentHashRing<>(VIRTUAL_NODES, initialNodes);
    }

    public String getTargetNode(String studentId) {
        return hashRing.get(studentId);
    }

    public Student saveStudent(Student student) {
        String targetNode = getTargetNode(student.getStudentId());
        MongoTemplate template = nodeManager.getTemplate(targetNode);
        return template.save(student);
    }

    public Student getStudent(String studentId) {
        String targetNode = getTargetNode(studentId);
        MongoTemplate template = nodeManager.getTemplate(targetNode);
        return template.findById(studentId, Student.class);
    }

    public synchronized void addNode(String nodeName, String uri) {
        nodeManager.registerNode(nodeName, uri, "student_db");
        Map<String, List<Student>> allData = fetchAllDataFromAllNodes();

        // Update the ring
        hashRing.add(nodeName);

        // Migrate affected records only
        for (Map.Entry<String, List<Student>> entry : allData.entrySet()) {
            String oldNode = entry.getKey();
            for (Student student : entry.getValue()) {
                String newNode = getTargetNode(student.getStudentId());
                if (!oldNode.equals(newNode)) {
                    nodeManager.getTemplate(newNode).save(student);
                    nodeManager.getTemplate(oldNode).remove(
                            Query.query(Criteria.where("_id").is(student.getStudentId())), Student.class
                    );
                }
            }
        }
    }

    public synchronized void removeNode(String nodeName) {
        MongoTemplate removeTemplate = nodeManager.getTemplate(nodeName);
        List<Student> orphanedStudents = removeTemplate.findAll(Student.class);

        // Remove node from ring
        hashRing.remove(nodeName);

        // Re-route orphaned records
        for (Student student : orphanedStudents) {
            String newTarget = getTargetNode(student.getStudentId());
            nodeManager.getTemplate(newTarget).save(student);
        }

        removeTemplate.dropCollection(Student.class);
        nodeManager.unregisterNode(nodeName);
    }

    public Map<String, Long> getDataDistribution() {
        Map<String, Long> stats = new HashMap<>();
        nodeManager.getAllTemplates().forEach((node, template) -> {
            stats.put(node, template.count(new Query(), Student.class));
        });
        return stats;
    }

    private Map<String, List<Student>> fetchAllDataFromAllNodes() {
        Map<String, List<Student>> map = new HashMap<>();
        nodeManager.getAllTemplates().forEach((node, template) -> {
            map.put(node, template.findAll(Student.class));
        });
        return map;
    }
}