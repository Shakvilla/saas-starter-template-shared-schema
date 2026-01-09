package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence;


import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class HibernateTenantFilterConfigurer {

    @PersistenceContext
    private EntityManager entityManager;


    public void enableTenantFilter(){
        String tenantId = TenantContext.getTenantId();

        if(tenantId == null){
            throw new IllegalStateException("TenantContext is missing when enabling Hibernate filter");
        }

        Session session = entityManager.unwrap(Session.class);

        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);
    }


    public void disableTenantFilter(){
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
    }
}
