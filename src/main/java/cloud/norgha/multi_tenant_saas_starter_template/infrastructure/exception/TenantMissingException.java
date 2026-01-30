package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception;

public class TenantMissingException extends RuntimeException {


    public TenantMissingException(String message){
        super(message);
    }



}
