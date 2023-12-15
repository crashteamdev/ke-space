ALTER TABLE ke_account_shop_item_strategy DROP CONSTRAINT fk_ke_account_shop_item_strategy_strategy_option;

ALTER TABLE ke_account_shop_item_strategy DROP COLUMN strategy_option_id;

ALTER TABLE strategy_option ADD ke_account_shop_item_strategy_id BIGSERIAL;

ALTER TABLE strategy_option
    ADD CONSTRAINT fk_strategy_option_ke_account_shop_item_strategy_id
        FOREIGN KEY (ke_account_shop_item_strategy_id) REFERENCES ke_account_shop_item_strategy (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX ke_account_shop_item_strategy_id_idx
    ON strategy_option (ke_account_shop_item_strategy_id);