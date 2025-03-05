Script for creating table that will hold dummy transactions
SQL Start
create table transactions
(
id                      integer      not null,
transaction_date        date         not null,
amount                  numeric      not null,
created_at              date,
constraint pk_transactions primary key(id)
);
SQL End

Script for creating 100mil of dummy transactions
SQL Start
DO $$
DECLARE
i INT := 1;
batch_size INT := 1000000; -- 1 million per batch
total_records INT := 100000000; -- 100 million
start_date DATE := '2020-01-01';
BEGIN
WHILE i <= total_records LOOP
INSERT INTO transactions (id, transaction_date, amount, created_at)
SELECT
i + row_number() OVER () AS id,
start_date + (random() * 1460)::int AS transaction_date, -- Random date within 4 years
CAST(random() * 1000 AS NUMERIC(10,2)) AS amount, -- Fix for ROUND()
now() AS created_at
FROM generate_series(1, batch_size);
i := i + batch_size;
RAISE NOTICE 'Inserted % records', i;
END LOOP;
END $$;
SQL End

