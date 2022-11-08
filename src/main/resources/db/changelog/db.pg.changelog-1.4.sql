--liquibase formatted sql
--changeset vitaxa:ke-account-sku-title
ALTER TABLE ke_account_shop ADD sku_title CHARACTER VARYING;
