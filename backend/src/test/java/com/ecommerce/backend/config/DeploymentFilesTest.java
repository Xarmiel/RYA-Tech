package com.ecommerce.backend.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeploymentFilesTest {
    @SuppressWarnings("unchecked")
    private Map<String,Object> parse(Path file) throws Exception {return new Yaml().load(Files.readString(file));}
    @SuppressWarnings("unchecked")
    @Test void blueprintsAgreeOnBuildRootHealthAndCheckedUpdates() throws Exception {
        var root=parse(Path.of("..","render.yaml"));var alternate=parse(Path.of("render.yaml"));
        assertEquals(root.get("services"),alternate.get("services"));
        var service=((List<Map<String,Object>>)root.get("services")).getFirst();
        assertEquals("backend",service.get("rootDir"));assertEquals("docker",service.get("runtime"));
        assertEquals("Dockerfile",service.get("dockerfilePath"));assertEquals(".",service.get("dockerContext"));
        assertEquals("/api/v1/health",service.get("healthCheckPath"));assertEquals("checksPass",service.get("autoDeployTrigger"));
        var variables=(List<Map<String,Object>>)service.get("envVars");
        for(String name:List.of("DB_URL","DB_USERNAME","DB_PASSWORD","CORS_ALLOWED_ORIGINS")){
            var entry=variables.stream().filter(item->name.equals(item.get("key"))).findFirst().orElseThrow();
            assertEquals(false,entry.get("sync"));assertFalse(entry.containsKey("value"));
        }
    }
    @SuppressWarnings("unchecked")
    @Test void workflowUsesJava21TestsAndImageBuildWithoutDeploymentSecrets() throws Exception {
        var workflow=parse(Path.of("..",".github","workflows","backend.yml"));
        var triggers=(Map<String,Object>)workflow.get("on");assertTrue(triggers.containsKey("push"));assertTrue(triggers.containsKey("pull_request"));
        var jobs=(Map<String,Object>)workflow.get("jobs");var job=(Map<String,Object>)jobs.get("verify");
        var steps=(List<Map<String,Object>>)job.get("steps");
        assertTrue(steps.stream().anyMatch(step->step.get("with") instanceof Map<?,?> settings&&"21".equals(settings.get("java-version"))));
        assertTrue(steps.stream().anyMatch(step->String.valueOf(step.get("run")).contains("verify")));
        assertTrue(steps.stream().anyMatch(step->String.valueOf(step.get("run")).contains("docker build")));
        assertFalse(Files.readString(Path.of("..",".github","workflows","backend.yml")).contains("secrets."));
    }
}
