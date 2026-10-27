package io.watermelon.ci.orchestration.service;

import io.watermelon.ci.domain.deployment.DeploymentStatus;
import io.watermelon.ci.fleet.service.FleetAdminService;
import io.watermelon.ci.orchestration.config.DemoProperties;
import io.watermelon.ci.runtime.model.AgentHeartbeatRequest;
import io.watermelon.ci.runtime.service.DeploymentTrackingService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * After platform is up, wait for DinD VMs and place sample containers so Services UI is not empty.
 * Uses public nginx:alpine so VMs look "alive" without a prior CI publish — that is not a registry artifact.
 */
@Component
public class DemoVmRuntimeSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoVmRuntimeSeeder.class);

    private final DemoProperties demoProperties;
    private final DemoBootstrapService bootstrapService;
    private final FleetAdminService fleetAdminService;
    private final DeploymentTrackingService deploymentTrackingService;

    public DemoVmRuntimeSeeder(
            DemoProperties demoProperties,
            DemoBootstrapService bootstrapService,
            FleetAdminService fleetAdminService,
            DeploymentTrackingService deploymentTrackingService) {
        this.demoProperties = demoProperties;
        this.bootstrapService = bootstrapService;
        this.fleetAdminService = fleetAdminService;
        this.deploymentTrackingService = deploymentTrackingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!demoProperties.isEnabled() || !demoProperties.isSeedRuntime()) {
            return;
        }
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "demo-vm-runtime-seeder");
            t.setDaemon(true);
            return t;
        }).execute(this::seedWithRetry);
    }

    private void seedWithRetry() {
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                UUID clusterId = bootstrapService.getDemoClusterId();
                UUID vmA = bootstrapService.getVmANodeId();
                UUID vmB = bootstrapService.getVmBNodeId();
                UUID storefrontId = bootstrapService.getDemoProjectId();
                UUID paymentsId = bootstrapService.getPaymentsProjectId();
                if (clusterId == null || vmA == null || vmB == null) {
                    bootstrapService.seed();
                    clusterId = bootstrapService.getDemoClusterId();
                    vmA = bootstrapService.getVmANodeId();
                    vmB = bootstrapService.getVmBNodeId();
                    storefrontId = bootstrapService.getDemoProjectId();
                    paymentsId = bootstrapService.getPaymentsProjectId();
                }
                // nginx:alpine is public — always pullable without our registry
                fleetAdminService.deployToNode(
                        clusterId, vmA, "storefront", "nginx:alpine",
                        List.of("8080:80"), "staging", "storefront", "seed-runtime", true);
                fleetAdminService.deployToNode(
                        clusterId, vmB, "payments", "nginx:alpine",
                        List.of("8081:80"), "staging", "payments", "seed-runtime", true);

                // Do NOT register nginx:alpine as a CI artifact — only track runtime.
                if (storefrontId != null) {
                    deploymentTrackingService.registerFromHeartbeat(
                            storefrontId,
                            new AgentHeartbeatRequest(
                                    null, "storefront", "staging", "nginx:alpine",
                                    "storefront", DeploymentStatus.HEALTHY, "seeded on demo-vm-a"));
                }
                if (paymentsId != null) {
                    deploymentTrackingService.registerFromHeartbeat(
                            paymentsId,
                            new AgentHeartbeatRequest(
                                    null, "payments", "staging", "nginx:alpine",
                                    "payments", DeploymentStatus.HEALTHY, "seeded on demo-vm-b"));
                }
                log.info("Demo runtime seeded: storefront→demo-vm-a, payments→demo-vm-b");
                return;
            } catch (Exception ex) {
                log.info("Demo VM seed attempt {}/30: {}", attempt, ex.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(4);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Demo VM runtime seed gave up — check demo-vm-a/b health");
    }
}
