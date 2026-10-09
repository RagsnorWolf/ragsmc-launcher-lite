package com.ragsmc.launcher.minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaManager {
    
    public static Optional<File> findJava8() {
        // Buscar Java 8 en ubicaciones comunes
        String[] paths = {
            "C:/Program Files/Java/jre1.8",
            "C:/Program Files/Java/jdk1.8",
            "C:/Program Files (x86)/Java/jre1.8",
            "C:/Program Files (x86)/Java/jdk1.8",
            "/usr/lib/jvm/java-8",
            "/usr/local/lib/java/jdk1.8"
        };
        
        for (String path : paths) {
            File javaDir = new File(path);
            if (javaDir.exists()) {
                // Encontrar la version mas reciente de Java 8 en este directorio
                File[] versions = javaDir.listFiles((dir, name) -> {
                    try {
                        return new File(dir, name).isDirectory() && 
                               name.matches("1\\.8\\.[0-9]+|jdk1\\.8\\.[0-9]+|jre1\\.8\\.[0-9]+");
                    } catch (Exception e) {
                        return false;
                    }
                });
                
                if (versions != null && versions.length > 0) {
                    // Ordenar por version y retornar la mas reciente
                    java.util.Arrays.sort(versions, (a, b) -> {
                        try {
                            String aVer = a.getName().replace("jdk", "").replace("jre", "").replace("1.", "").replace(".", "");
                            String bVer = b.getName().replace("jdk", "").replace("jre", "").replace("1.", "").replace(".", "");
                            return bVer.compareTo(aVer);
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    
                    return Optional.of(versions[0]);
                }
            }
        }
        
        return Optional.empty();
    }
    
    public static String getJavaVersionFromRuntime() {
        String version = System.getProperty("java.version");
        if (version != null) {
            return version.split("\\.")[0] + "." + version.split("\\.")[1];
        }
        return "unknown";
    }
}
