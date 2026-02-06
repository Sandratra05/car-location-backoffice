package com.example;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;


public class Main {

    public static void main(String[] args) throws LifecycleException, IOException, URISyntaxException {
        // Port configurable via variable d'environnement (Render utilise PORT)
        String portEnv = System.getenv("PORT");
        int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        
        // Nécessaire pour que Tomcat démarre correctement
        tomcat.getConnector();

        // Créer un répertoire temporaire pour le webapp
        Path tempDir = Files.createTempDirectory("tomcat-webapp");
        File webappDir = tempDir.toFile();
        
        // Extraire les fichiers webapp du JAR vers le répertoire temporaire
        extractWebapp(webappDir);

        // Utiliser addContext au lieu de addWebapp pour éviter la lecture du web.xml
        Context context = tomcat.addWebapp("", webappDir.getAbsolutePath());
        
        // Désactiver le rechargement automatique en production
        context.setReloadable(false);

        // Configurer les ressources pour les classes compilées
        WebResourceRoot resources = new StandardRoot(context);
        
        // Ajouter le chemin des classes (pour le ClassLoader)
        File classesLocation = new File(Main.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        
        if (classesLocation.isFile()) {
            // Si c'est un JAR, on est en mode production
            System.out.println("Running from JAR: " + classesLocation.getAbsolutePath());
            // Ajouter le JAR comme ressource pour que les classes soient accessibles
            resources.addJarResources(new JarResourceSet(resources, "/WEB-INF/classes",
                    classesLocation.getAbsolutePath(), "/"));
        } else {
            // Mode développement - classes dans un répertoire
            System.out.println("Running from classes directory: " + classesLocation.getAbsolutePath());
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    classesLocation.getAbsolutePath(), "/"));
        }
        
        context.setResources(resources);

        System.out.println("===========================================");
        System.out.println("  Backoffice Car Location - Démarrage");
        System.out.println("===========================================");
        System.out.println("  Port: " + port);
        System.out.println("  Webapp: " + webappDir.getAbsolutePath());
        System.out.println("===========================================");

        tomcat.start();
        System.out.println("Serveur démarré sur http://localhost:" + port);
        //System.out.println("  - http://localhost:" + port + "/test");
        //System.out.println("  - http://localhost:" + port + "/hello");
        tomcat.getServer().await();
    }

    /**
     * Extrait les fichiers webapp du classpath vers un répertoire temporaire
     * Parcourt automatiquement tous les fichiers du dossier webapp/
     */
    private static void extractWebapp(File targetDir) throws IOException, URISyntaxException {
        String resourcePath = "webapp";
        URL resourceUrl = Main.class.getClassLoader().getResource(resourcePath);
        
        if (resourceUrl == null) {
            System.err.println("Webapp resources not found!");
            return;
        }

        URI uri = resourceUrl.toURI();
        
        if (uri.getScheme().equals("jar")) {
            // Mode JAR: parcourir les fichiers à l'intérieur du JAR
            try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path webappPath = fileSystem.getPath("/" + resourcePath);
                extractDirectory(webappPath, targetDir.toPath(), resourcePath);
            }
        } else {
            // Mode développement: parcourir le répertoire local
            Path webappPath = Paths.get(uri);
            extractDirectory(webappPath, targetDir.toPath(), resourcePath);
        }
    }

    /**
     * Extrait récursivement tous les fichiers d'un répertoire
     */
    private static void extractDirectory(Path sourcePath, Path targetPath, String resourceBasePath) throws IOException {
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.forEach(source -> {
                try {
                    // Calculer le chemin relatif par rapport au dossier webapp
                    String relativePath = sourcePath.relativize(source).toString();
                    
                    // Ignorer le répertoire racine (chemin vide)
                    if (relativePath.isEmpty()) {
                        return;
                    }
                    
                    Path target = targetPath.resolve(relativePath);
                    
                    if (Files.isDirectory(source)) {
                        // Créer le répertoire s'il n'existe pas
                        Files.createDirectories(target);
                    } else {
                        // Copier le fichier
                        Files.createDirectories(target.getParent());
                        try (InputStream is = Files.newInputStream(source)) {
                            Files.copy(is, target);
                            System.out.println("Extracted: " + relativePath);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error extracting: " + source + " - " + e.getMessage());
                }
            });
        }
    }
}
