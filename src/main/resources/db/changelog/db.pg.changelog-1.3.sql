--liquibase formatted sql
--changeset vitaxa:create-category-index
CREATE INDEX IF NOT EXISTS ke_shop_item_category_id_index
    ON ke_shop_item (category_id);

--changeset vitaxa:create-p_hash-index
CREATE INDEX IF NOT EXISTS ke_shop_item_p_hash_fingerprint_index
    on ke_shop_item (p_hash_fingerprint);

