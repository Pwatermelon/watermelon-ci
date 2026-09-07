package io.watermelon.ci.identity.service;

import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.ids.Ids;
import io.watermelon.ci.common.time.Clock;
import io.watermelon.ci.common.util.Slugs;
import io.watermelon.ci.domain.identity.AccessGroup;
import io.watermelon.ci.domain.identity.AccessGroupRepository;
import io.watermelon.ci.domain.identity.PlatformRole;
import io.watermelon.ci.domain.identity.ProjectMembership;
import io.watermelon.ci.domain.identity.ProjectMembershipRepository;
import io.watermelon.ci.identity.rbac.RoleHierarchy;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessControlService {

    private final AccessGroupRepository groupRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final Clock clock = Clock.system();

    public AccessControlService(
            AccessGroupRepository groupRepository, ProjectMembershipRepository membershipRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public AccessGroup createGroup(UUID organizationId, String name, PlatformRole defaultRole) {
        String slug = Slugs.of(name);
        AccessGroup group = new AccessGroup(Ids.newId(), organizationId, slug, name, defaultRole, clock.now());
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<AccessGroup> listGroups(UUID organizationId) {
        return groupRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public ProjectMembership grant(UUID projectId, String subject, PlatformRole role) {
        return membershipRepository
                .findByProjectIdAndSubject(projectId, subject)
                .orElseGet(() -> membershipRepository.save(
                        new ProjectMembership(Ids.newId(), projectId, subject, role, clock.now())));
    }

    @Transactional(readOnly = true)
    public void require(UUID projectId, String subject, PlatformRole required) {
        ProjectMembership membership = membershipRepository
                .findByProjectIdAndSubject(projectId, subject)
                .orElseThrow(() -> new NotFoundException("no membership for subject on project"));
        if (!RoleHierarchy.atLeast(membership.getRole(), required)) {
            throw new io.watermelon.ci.common.error.PlatformException(
                    io.watermelon.ci.common.error.ErrorCode.FORBIDDEN,
                    "role " + membership.getRole() + " is below required " + required);
        }
    }
}
