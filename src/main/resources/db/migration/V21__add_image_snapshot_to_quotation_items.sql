-- Add image_path_snapshot to quotation_items
-- Purpose: Store product image at quotation time for PDF generation

-- Add new column (nullable, safe to add)
ALTER TABLE quotation_items 
ADD COLUMN IF NOT EXISTS image_path_snapshot TEXT;

-- Backfill existing quotation items with current product images
UPDATE quotation_items qi
SET image_path_snapshot = (
    SELECT p.image_path 
    FROM products p 
    WHERE p.id = qi.product_id
)
WHERE qi.product_id IS NOT NULL 
  AND qi.image_path_snapshot IS NULL;

-- Add comment
COMMENT ON COLUMN quotation_items.image_path_snapshot IS 'Snapshot of product image at quotation time for PDF generation';
