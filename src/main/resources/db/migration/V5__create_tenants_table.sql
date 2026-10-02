CREATE TABLE public.tenants (
                                id UUID PRIMARY KEY,
                                name VARCHAR(100) NOT NULL,
                                schema_name VARCHAR(100) NOT NULL,

                                CONSTRAINT uk_tenants_name
                                    UNIQUE (name),

                                CONSTRAINT uk_tenants_schema_name
                                    UNIQUE (schema_name)
);