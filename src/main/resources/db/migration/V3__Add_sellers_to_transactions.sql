ALTER TABLE transactions
    ADD COLUMN seller_company_id BIGINT;

ALTER TABLE transactions
    ADD COLUMN seller_contact_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_txn_seller_comp FOREIGN KEY (seller_company_id) REFERENCES companies(id);

ALTER TABLE transactions
    ADD CONSTRAINT fk_txn_seller_cont FOREIGN KEY (seller_contact_id) REFERENCES contacts(id);