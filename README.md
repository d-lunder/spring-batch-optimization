Script for creating table that will hold dummy transactions
```sql
create table transactions
(
id                      integer      not null,
transaction_date        date         not null,
amount                  numeric      not null,
created_at              date,
constraint pk_transactions primary key(id)
);
```

Script for creating 1 million of dummy transactions
```sql
DO $$
DECLARE
i INT := 1;
batch_size INT := 10000; -- 10 000
total_records INT := 1000000; -- 1 000 000
start_date DATE := '2020-01-01';
BEGIN
WHILE i <= total_records LOOP
INSERT INTO transactions (id, transaction_date, amount, created_at)
SELECT
i + row_number() OVER () AS id,
start_date + (random() * 1460)::int AS transaction_date,
CAST(random() * 1000 AS NUMERIC(10,2)) AS amount,
now() AS created_at
FROM generate_series(1, batch_size);
i := i + batch_size;
RAISE NOTICE 'Inserted % records', i-1;
END LOOP;
END $$;
```

