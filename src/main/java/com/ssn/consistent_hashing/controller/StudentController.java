package com.ssn.consistent_hashing.controller;

import com.ssn.consistent_hashing.model.Student;
import com.ssn.consistent_hashing.service.StudentRoutingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRoutingService routingService;

    public StudentController(StudentRoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping
    public Student addStudent(@RequestBody Student student) {
        return routingService.saveStudent(student);
    }

    @GetMapping("/{id}")
    public Student getStudent(@PathVariable String id) {
        return routingService.getStudent(id);
    }

    @GetMapping("/{id}/target-node")
    public String getTargetNode(@PathVariable String id) {
        return routingService.getTargetNode(id);
    }

    @GetMapping("/distribution")
    public Map<String, Long> getDistribution() {
        return routingService.getDataDistribution();
    }

    @PostMapping("/nodes/add")
    public String addNode(@RequestParam String name, @RequestParam String uri) {
        routingService.addNode(name, uri);
        return "Node " + name + " added successfully.";
    }

    @DeleteMapping("/nodes/remove/{name}")
    public String removeNode(@PathVariable String name) {
        routingService.removeNode(name);
        return "Node " + name + " removed successfully.";
    }
}