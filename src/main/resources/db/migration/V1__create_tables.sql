create table if not exists stock
(
    id              bigint not null
        primary key,
    remain_quantity bigint null
);

create table if not exists stock_seq
(
    next_val bigint null
);

create table if not exists stock_with_optimistic_lock
(
    id              bigint not null
        primary key,
    remain_quantity bigint null,
    version         int    not null
);


create table if not exists stock_with_optimistic_lock_seq
(
    next_val bigint null
);
