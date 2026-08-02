#!/bin/bash
# Production Build Script for QuoteFlow Backend
# This ensures all dependencies including PostgreSQL driver are packaged correctly

set -e  # Exit on any error

echo "=========================================="
echo "QuoteFlow Production Build"
echo "=========================================="
echo ""

# Step 1: Clean previous builds
echo "[1/5] Cleaning previous builds..."
./mvnw clean

# Step 2: Compile the code
echo ""
echo "[2/5] Compiling source code..."
./mvnw compile -DskipTests

# Step 3: Package the application (this includes all runtime dependencies)
echo ""
echo "[3/5] Packaging application with all dependencies..."
./mvnw package -DskipTests

# Step 4: Verify the JAR was created
echo ""
echo "[4/5] Verifying JAR file..."
JAR_FILE="target/quotation-0.0.1-SNAPSHOT.jar"
if [ -f "$JAR_FILE" ]; then
    echo "✓ JAR file created successfully: $JAR_FILE"
    echo "  Size: $(du -h $JAR_FILE | cut -f1)"
else
    echo "✗ ERROR: JAR file not found!"
    exit 1
fi

# Step 5: Verify PostgreSQL driver is included
echo ""
echo "[5/5] Verifying PostgreSQL driver is included..."
if jar tf "$JAR_FILE" | grep -q "org/postgresql/Driver.class"; then
    echo "✓ PostgreSQL JDBC driver is included in JAR"
else
    echo "✗ WARNING: PostgreSQL driver not found in JAR!"
    echo "  This will cause 'Failed to determine a suitable driver class' error"
    exit 1
fi

echo ""
echo "=========================================="
echo "Build completed successfully!"
echo "=========================================="
echo ""
echo "JAR Location: $JAR_FILE"
echo ""
echo "Next steps:"
echo "1. Upload to server: scp $JAR_FILE root@187.127.172.63:/tmp/"
echo "2. Deploy on server:"
echo "   systemctl stop quoteflow"
echo "   mv /tmp/quotation-0.0.1-SNAPSHOT.jar /opt/quoteflow/"
echo "   chown quoteflow:quoteflow /opt/quoteflow/quotation-0.0.1-SNAPSHOT.jar"
echo "   chmod 750 /opt/quoteflow/quotation-0.0.1-SNAPSHOT.jar"
echo "   systemctl start quoteflow"
echo "   systemctl status quoteflow"
echo ""
