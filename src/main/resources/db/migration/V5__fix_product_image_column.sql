-- Migration V5: Fix product image_path column to support base64 images
-- Base64 images are typically 10KB-5MB, which translates to ~13KB-7MB in base64
-- Using TEXT type for unlimited length

-- Change image_path from VARCHAR(500) to TEXT
ALTER TABLE products 
ALTER COLUMN image_path TYPE TEXT;

-- Add comment to document the change
COMMENT ON COLUMN products.image_path IS 'Stores base64 encoded image data or file path. TEXT type supports unlimited length for base64 images.';
