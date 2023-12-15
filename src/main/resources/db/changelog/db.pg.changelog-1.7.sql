ALTER TABLE ke_account_shop_item DROP CONSTRAINT fk_ke_account_shop_item_strategy_id_ke_account_shop_item;

DROP INDEX ke_account_shop_item_shop_item_strategy_id_idx;

ALTER TABLE ke_account_shop_item DROP COLUMN ke_account_shop_item_strategy_id;

ALTER TABLE ke_account_shop_item_strategy ADD ke_account_shop_item_id uuid;

ALTER TABLE ke_account_shop_item_strategy
    ADD CONSTRAINT fk_ke_account_shop_item_id_ke_account_shop_item_strategy_id
        FOREIGN KEY (ke_account_shop_item_id) REFERENCES ke_account_shop_item (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX shop_item_strategy_ke_account_shop_item_id_idx
    ON ke_account_shop_item_strategy (ke_account_shop_item_id);