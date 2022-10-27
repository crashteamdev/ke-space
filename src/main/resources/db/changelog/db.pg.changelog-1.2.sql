ALTER TABLE ke_account_shop_item_competitor
    DROP CONSTRAINT fk_ke_account_shop_item_competitor_ke_account_shop_item;

ALTER TABLE ke_account_shop_item_competitor
    ADD CONSTRAINT fk_ke_account_shop_item_competitor_ke_account_shop_item
        FOREIGN KEY (ke_account_shop_item_id) REFERENCES ke_account_shop_item
            ON DELETE CASCADE;

