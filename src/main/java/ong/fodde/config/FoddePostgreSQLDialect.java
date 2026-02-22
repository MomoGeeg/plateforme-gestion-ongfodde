package ong.fodde.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;

public class FoddePostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, 
                                ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.getTypeConfiguration()
            .getJdbcTypeRegistry()
            .addDescriptor(SqlTypes.UUID, 
                org.hibernate.type.descriptor.jdbc.UUIDJdbcType.INSTANCE);
    }
}