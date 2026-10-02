ALTER TABLE public.tenants
    ADD COLUMN slug VARCHAR(100) NOT NULL;

ALTER TABLE public.tenants
    ADD CONSTRAINT uk_tenants_slug UNIQUE (slug);