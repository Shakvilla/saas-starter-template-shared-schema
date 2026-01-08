package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant;

import jakarta.servlet.http.HttpServletRequest;

public class TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    public String resolveTenant(HttpServletRequest request){

        String tenantId = request.getHeader(TENANT_HEADER);

        if(tenantId == null || tenantId.isBlank()){
            throw new TenantMissingException("Missing X-Tenant-ID header");
        }


        return tenantId;
    }
}
