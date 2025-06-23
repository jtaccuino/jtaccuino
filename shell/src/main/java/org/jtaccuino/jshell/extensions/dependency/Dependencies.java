/*
 * Copyright 2024-2025 JTaccuino Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.jshell.extensions.dependency;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.RepositoryOfflineException;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

final class Dependencies {

    private static RepositorySystem newRepositorySystem() {
        var repoSup = new RepositorySystemSupplier();
        return repoSup.get();
    }

    public static RepositorySystemSession.SessionBuilder newRepositorySystemSession(RepositorySystem system) {
        RepositorySystemSession.SessionBuilder result = new SessionBuilderSupplier(system)
                .get()
                .withLocalRepositoryBaseDirectories(Path.of(System.getProperty("user.home")
                        + File.separator + "jtaccuino" + File.separator + "cache" + File.separator + "local-repo"))
                .setConfigProperty("aether.syncContext.named.factory", "noop");
        return result;
    }

    static DependencyArtifact resolve(String mavenCoordinates) {
        try {
            RepositorySystem repoSystem = newRepositorySystem();

            RepositorySystemSession session = newRepositorySystemSession(repoSystem).build();

            var artifact = new DefaultArtifact(mavenCoordinates);

            Dependency dependency
                    = new Dependency(artifact, "compile");
            RemoteRepository central = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();

            var localRepoPath = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
            RemoteRepository mavenlocal = new RemoteRepository.Builder("mavenLocal", "default", new File(localRepoPath).toURI().toString()).build();

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.addRepository(mavenlocal);
            collectRequest.addRepository(central);

            DependencyFilter javafxFilter = (dn, list) -> {
                return !"org.openjfx".equals(dn.getArtifact().getGroupId());
            };

            DependencyFilter compileFilter = (dn, list) -> {
                return !"compile".equals(dn.getDependency().getScope());
            };

            DependencyFilter runtimeFilter = (dn, list) -> {
                return !"runtimeFilter".equals(dn.getDependency().getScope());
            };

            var filter = DependencyFilterUtils.andFilter(javafxFilter, DependencyFilterUtils.orFilter(compileFilter, runtimeFilter));

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

            var dependencyResult = repoSystem.resolveDependencies(session, dependencyRequest);

            var dv = new DependencyVisitor() {

                List<DependencyArtifact> deps = new ArrayList<>();

                @Override
                public boolean visitEnter(DependencyNode node) {
                    DependencyArtifact newDep = new DependencyArtifact(
                            node.getDependency().getArtifact().getGroupId(),
                            node.getDependency().getArtifact().getArtifactId(),
                            node.getDependency().getArtifact().getClassifier(),
                            node.getDependency().getArtifact().getBaseVersion(),
                            node.getDependency().getArtifact().getVersion(),
                            node.getDependency().getArtifact().getPath(),
                            new ArrayList<>());
                    if (!deps.isEmpty()) {
                        deps.getLast().dependencies().add(newDep);
                    }
                    deps.addLast(newDep);
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode dn) {
                    if (1 < deps.size()) {
                        deps.removeLast();
                    }
                    return true;
                }

            };
            dependencyResult.getRoot()
                    .accept(dv);
            return dv.deps.getLast();
        } catch (DependencyResolutionException ex) {
            var message = ex.getResult().getArtifactResults().stream()
                    .filter(ar -> !ar.isResolved())
                    .flatMap(ar -> ar.getExceptions().stream())
                    .map(e -> switch (e) {
                case ArtifactNotFoundException anfe ->
                    "Artifact not found " + anfe.getMessage();
                case RepositoryOfflineException roe ->
                    roe.getRepository().toString();
                default ->
                    ex.getMessage();
            })
                    .collect(Collectors.joining("\n"));
            throw new DependencyException("Failure adding dependencies:\n" + message);
        }
    }
}
