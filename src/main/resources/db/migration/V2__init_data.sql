TRUNCATE TABLE stock;
ALTER TABLE stock AUTO_INCREMENT = 1;

TRUNCATE TABLE stock_with_optimistic_lock;
ALTER TABLE stock_with_optimistic_lock AUTO_INCREMENT = 1;

INSERT INTO `stock-study`.stock (id, remain_quantity)
VALUES (1, 1000000);
INSERT INTO `stock-study`.stock (id, remain_quantity)
VALUES (2, 1000000);
INSERT INTO `stock-study`.stock (id, remain_quantity)
VALUES (3, 1000000);

INSERT INTO `stock-study`.stock_seq (next_val)
VALUES (1);

INSERT INTO `stock-study`.stock_with_optimistic_lock (id, remain_quantity, version)
VALUES (1, 1000000, 0);
INSERT INTO `stock-study`.stock_with_optimistic_lock (id, remain_quantity, version)
VALUES (2, 1000000, 0);
INSERT INTO `stock-study`.stock_with_optimistic_lock (id, remain_quantity, version)
VALUES (3, 1000000, 0);

INSERT INTO `stock-study`.stock_with_optimistic_lock_seq (next_val)
VALUES (1);
