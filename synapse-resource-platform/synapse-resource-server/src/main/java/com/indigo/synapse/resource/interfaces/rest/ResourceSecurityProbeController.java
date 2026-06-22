package com.indigo.synapse.resource.interfaces.rest;

import com.indigo.synapse.core.context.OperationActor;
import com.indigo.synapse.core.context.OperationContext;
import com.indigo.synapse.core.context.OperationContextHolder;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import com.indigo.synapse.security.context.CurrentPrincipalContext;
import com.indigo.synapse.security.permission.PermissionChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway P0 验收使用的最小受保护端点。
 *
 * <p>这些端点不实现 Resource 领域业务，只用于证明下游独立 JWT 验证、GatewayProof 来源证明、权限拒绝、
 * CurrentPrincipalContext 与 OperationContext 已形成闭环。</p>
 */
@RestController
@RequestMapping("/internal/security")
public class ResourceSecurityProbeController {

    /** P0 验收权限。 */
    public static final String READ_PERMISSION = "resource:security:read";

    private final PermissionChecker permissionChecker;

    public ResourceSecurityProbeController(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    /**
     * 仅要求 JWT 与 GatewayProof 均有效。
     *
     * @return 固定验收响应
     */
    @GetMapping("/protected")
    public ProbeResponse protectedEndpoint() {
        return new ProbeResponse("ok");
    }

    /**
     * 额外要求 Token 权限快照包含 {@value READ_PERMISSION}。
     *
     * @return 固定验收响应
     */
    @GetMapping("/permission")
    public ProbeResponse permissionEndpoint() {
        permissionChecker.require(READ_PERMISSION);
        return new ProbeResponse("ok");
    }

    /**
     * 返回 Framework 已建立的主体和操作上下文最小视图。
     *
     * @return 只包含技术验收字段的上下文视图
     */
    @GetMapping("/context")
    public SecurityContextView contextEndpoint() {
        AuthenticatedPrincipal principal = CurrentPrincipalContext.currentPrincipal()
                .orElseThrow(() -> new IllegalStateException("principal context is not available"));
        OperationContext operationContext = OperationContextHolder.requireCurrent();
        OperationActor actor = operationContext.actor();
        OperationActor initiator = operationContext.initiator();
        return new SecurityContextView(
                principal.principalId(),
                actor == null ? null : actor.id(),
                initiator == null ? null : initiator.id(),
                operationContext.tenantId()
        );
    }

    /** P0 固定响应。 */
    public record ProbeResponse(String status) {
    }

    /** Framework 上下文的最小只读验收投影。 */
    public record SecurityContextView(
            String principalId,
            String actorId,
            String initiatorId,
            String tenantId) {
    }
}
