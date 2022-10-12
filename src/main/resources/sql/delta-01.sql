alter table trade add column piso bigint not null default 1;
alter table monedamonto add column piso bigint not null default 1;

GRANT ALL PRIVILEGES ON TABLE trade TO juan;
GRANT ALL PRIVILEGES ON TABLE monedamonto TO juan;