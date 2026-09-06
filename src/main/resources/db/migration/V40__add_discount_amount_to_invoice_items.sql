ALTER TABLE invoice_items
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0;

-- First: use the exact discount stored on quotation_items
UPDATE invoice_items ii
SET discount_amount = qi.discount_amount
FROM invoice i
JOIN quotation q ON q.id = i.quotation_id
JOIN quotation_items qi ON qi.quotation_id = q.id
WHERE ii.invoice_id = i.id
  AND qi.product_id = ii.product_id
  AND qi.discount_amount IS NOT NULL
  AND qi.discount_amount > 0;

-- Then: calculate discount for remaining invoice items
UPDATE invoice_items ii
SET discount_amount = GREATEST(
    0,
    (ii.unit_price * ii.quantity) -
    (ii.total - COALESCE(ii.tax_amount, 0))
)
WHERE ii.discount_amount = 0
  AND ii.total IS NOT NULL;