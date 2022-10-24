--liquibase formatted sql
--changeset vitaxa:create-bitcount-func
CREATE OR REPLACE FUNCTION bit_count(i integer) RETURNS integer AS '
DECLARE n integer;
    DECLARE amount integer;
BEGIN
    amount := 0;
    FOR n IN 1..16 LOOP
            amount := amount + ((i >> (n-1)) & 1);
        END LOOP;
    RETURN amount;
END
' LANGUAGE plpgsql;

--changeset vitaxa:create-ke_account_shop_item_name_idx
CREATE INDEX IF NOT EXISTS ke_account_shop_item_name ON ke_account_shop_item (name);
