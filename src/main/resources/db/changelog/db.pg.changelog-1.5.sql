CREATE TABLE strategy_option
(
    id                BIGSERIAL PRIMARY KEY,
    minimum_threshold BIGINT,
    maximum_threshold BIGINT
);

CREATE TABLE ke_account_shop_item_strategy
(

    id                 BIGSERIAL PRIMARY KEY,
    strategy_name      VARCHAR(64),
    strategy_option_id BIGINT,

    CONSTRAINT fk_ke_account_shop_item_strategy_strategy_option
        FOREIGN KEY (strategy_option_id) REFERENCES strategy_option (id) ON DELETE CASCADE
);

CREATE INDEX strategy_options_id_index ON ke_account_shop_item_strategy (strategy_option_id);

ALTER TABLE ke_account_shop_item
    ADD ke_account_shop_item_strategy_id BIGINT;

ALTER TABLE ke_account_shop_item
    ADD CONSTRAINT fk_ke_account_shop_item_strategy_id_ke_account_shop_item
        FOREIGN KEY (ke_account_shop_item_strategy_id) REFERENCES ke_account_shop_item_strategy (id) ON DELETE CASCADE;