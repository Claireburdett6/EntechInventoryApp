# Entech Controls Inventory (Android)

Native Android inventory management prototype built with Kotlin and Android's SQLite database APIs.

## Open in Android Studio
1. Extract the ZIP file.
2. Open Android Studio.
3. Choose **Open** and select the `EntechInventoryAndroid` folder (the folder containing `settings.gradle.kts`).
4. Allow Gradle to sync. Use JDK 17 for Gradle if Android Studio asks.
5. Create or select an Android emulator (API 23 or newer; API 35 recommended).
6. Press **Run**.

## Features
- Dashboard with item count, total quantity, and low-stock count
- Add, edit, and delete inventory records
- Search by item name, part number, or supplier
- Receive stock and record usage
- Prevents duplicate part numbers and negative stock
- Current inventory, low-stock, recent receipts, and usage reports
- SQLite database persists data on the device
- Five sample records are inserted on first launch

## Project structure
- `MainActivity.kt`: native Android screens, forms, navigation, and user interactions
- `InventoryDatabase.kt`: SQLite tables and CRUD / stock movement operations
- `AndroidManifest.xml`: Android app declaration
- `styles.xml`: app theme

## Notes
This is a local-device prototype. Data is saved on the emulator/device and is not synchronized between employees' devices. A shared production system would require a backend and shared database.

## Test checklist
- Add an item with a unique part number.
- Try a duplicate part number; saving should be rejected.
- Edit an existing item.
- Delete an item.
- Receive stock and verify quantity increases.
- Record usage and verify quantity decreases.
- Try using more stock than available; the transaction should be rejected.
- Search by name, part number, and supplier.
- Close and reopen the app; verify the data remains.
