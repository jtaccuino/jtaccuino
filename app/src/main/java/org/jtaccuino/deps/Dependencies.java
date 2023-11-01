package org.jtaccuino.deps;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Dependencies {

    @SuppressWarnings("deprecation")
    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("./cache/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    public static List<Path> resolve(String mavenCoordinates) {
        try {
            RepositorySystem repoSystem = newRepositorySystem();

            RepositorySystemSession session = newSession(repoSystem);

            Dependency dependency
                    = new Dependency(new DefaultArtifact(mavenCoordinates), "compile");
            RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.addRepository(central);
            DependencyNode node;
            node = repoSystem.collectDependencies(session, collectRequest).getRoot();

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);

            repoSystem.resolveDependencies(session, dependencyRequest);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);
            System.out.println("Dependencies");
            return nlg.getArtifacts(false).stream().map(a -> a.getFile().toPath()).toList();
        } catch (DependencyCollectionException | DependencyResolutionException ex) {
            Logger.getLogger(Dependencies.class.getName()).log(Level.SEVERE, null, ex);
            return Collections.emptyList();
        }
    }
}
