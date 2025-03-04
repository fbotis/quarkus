
package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

//
// The purpose of this test is to assert that
// When: We run an in-cluster  container builds targeting Openshift (and `Deployment` is used).
// Then:
//   - A BuildConfg is generated.
//   - Two ImageStream are generated (one named after the app).
//   - A Deployment resource was created
//   - image-registry.openshift-image-registry.svc:5000 is used as registry (why? so that Deployment can point to the incluster built image).
//
public class OpenshiftWithDeploymentResourceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("openshift-with-deployment-resource")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.openshift.deployment-kind", "Deployment")
            .overrideConfigKey("quarkus.openshift.replicas", "3")
            .overrideConfigKey("quarkus.container-image.group", "testme")
            .setLogFileName("k8s.log")
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-openshift", Version.getVersion())));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        final Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("openshift.yml"));
        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("openshift.yml"));

        assertThat(kubernetesList).filteredOn(h -> "BuildConfig".equals(h.getKind())).hasSize(1);
        assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())).hasSize(2);
        assertThat(kubernetesList).filteredOn(h -> "ImageStream".equals(h.getKind())
                && h.getMetadata().getName().equals("openshift-with-deployment-resource")).hasSize(1);

        assertThat(kubernetesList).filteredOn(i -> i instanceof Deployment).singleElement().satisfies(i -> {
            assertThat(i).isInstanceOfSatisfying(Deployment.class, d -> {
                assertThat(d.getMetadata()).satisfies(m -> {
                    assertThat(m.getName()).isEqualTo("openshift-with-deployment-resource");
                });

                assertThat(d.getSpec()).satisfies(deploymentSpec -> {
                    assertThat(deploymentSpec.getReplicas()).isEqualTo(3);
                    assertThat(deploymentSpec.getTemplate()).satisfies(t -> {
                        assertThat(t.getSpec()).satisfies(podSpec -> {
                            assertThat(podSpec.getContainers()).singleElement().satisfies(container -> {
                                assertThat(container.getImage())
                                        .isEqualTo(
                                                "image-registry.openshift-image-registry.svc:5000/testme/openshift-with-deployment-resource:0.1-SNAPSHOT");
                            });
                        });
                    });
                });
            });
        });
    }
}
