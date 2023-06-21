CREATE TYPE strategy_type AS ENUM ('close_to_minimal','quantity_dependent');

CREATE TABLE strategy_option
(
    id                BIGSERIAL PRIMARY KEY,
    minimum_threshold BIGINT,
    maximum_threshold BIGINT
);

CREATE TABLE ke_account_shop_item_strategy
(

    id                 BIGSERIAL PRIMARY KEY,
    strategy_type      strategy_type NOT NULL DEFAULT 'close_to_minimal',
    strategy_option_id BIGINT,

    CONSTRAINT fk_ke_account_shop_item_strategy_strategy_option
        FOREIGN KEY (strategy_option_id) REFERENCES strategy_option (id) ON DELETE CASCADE
);

ALTER TABLE ke_account_shop_item
    ADD ke_account_shop_item_strategy_id BIGINT;

ALTER TABLE ke_account_shop_item
    ADD CONSTRAINT fk_ke_account_shop_item_strategy_id_ke_account_shop_item
        FOREIGN KEY (ke_account_shop_item_strategy_id) REFERENCES ke_account_shop_item_strategy (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX ke_account_shop_item_shop_item_strategy_id_idx ON ke_account_shop_item (ke_account_shop_item_strategy_id);
