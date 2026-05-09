.\gradlew assembleDebug


Here's a clean **Product Requirements Document (PRD)** for your **custom Food Stall POS app**. It's structured and formatted professionally, ready to be shared with developers, designers, or stakeholders.


# 📝 Product Requirements Document (PRD)

## Project Name
**Food Stall POS (Offline Point-of-Sale System)**

## Prepared For
**Single Food Stall Client**

## Prepared By
Jyot Raval
Genral Purpose.

---

## 1. 🎯 Objective

To design and develop a **lightweight, fully offline Android POS app** for a food stall vendor. The application must enable quick order processing, receipt printing via Bluetooth, and provide basic analytics (sales dashboard, top items) — all without relying on internet connectivity or cloud storage.

---

## 2. 👥 Target Users

- Stall Owner / Cashier operating a single Android tablet
- Non-technical user; prefers simple UI/UX
- Needs fast billing and receipt generation
- No need for multi-user or cloud management

---

## 3. ✅ Key Features

### 3.1 App Access
- **PIN Lock Screen** (Default: `1111`)
- Changeable via settings
- No timeout or biometric auth

---

### 3.2 Main Menu / Navigation
Landing screen includes buttons to:
- Inventory
- Billing
- Dashboard
- Past Transactions
- ⚙️ Settings (top-right corner)

---

### 3.3 Inventory Management
- Add/Edit/Delete Categories
- Add/Edit/Delete Items
  - Fields: Item Name, Category, Price
- No stock/inventory tracking

---

### 3.4 Billing Module (Core Feature)

#### Tile-Based UI
- Items shown in rectangular tiles
- Grouped by category
- Tap anywhere to add to cart
- Left `➖` and right `➕` to adjust quantity
- Green border + faded text for selected items
- Manual quantity input for large orders

#### Cart Window
- Shows Item Name, Qty, Unit Price, Line Total
- Optional fields:
  - Buyer Name
  - Buyer Phone Number
- Apply discount:
  - Fixed amount (₹)
  - Percentage (%)

#### Actions
- **Print Receipt**: Triggers Bluetooth printer
- **Clear Cart**: Resets billing window

---

### 3.5 Receipt Printing

#### Printing Method
- ESC/POS over Bluetooth
- Configured using MAC address in Settings
- One-time pairing only

#### Receipt Layout

[ Stall Name ]
[ Address ]
[ Phone Number ]
----------------

Date: DD-MM-YYYY  Time: HH:MM
Txn ID: TXNYYYYMMDD-001
Buyer: Name (Phone)
-------------------

## Item         Qty  Price  Total

Burger       2    40     80
Tea          1    10     10
---------------------------

Subtotal:                    90
Discount:                   -10
Total:                    **80**


#### Receipt Settings
- Padding (top/bottom)
- Width setting (optional)
- **Test Print** button for alignment checking

---

### 3.6 Dashboard

#### Day-wise / Month-wise Sales Chart
- Toggle:
  - Last 30 Days (daily)
  - Last 12 Months (monthly)

#### Top-Selling Items
- Filters:
  - Today
  - Last 7 Days
  - Last 30 Days
  - Custom Date Range
- Display:
  - Item Name, Quantity Sold, Total Revenue

---

### 3.7 Past Transactions

#### Transaction History
- Grouped by Date
- Each row shows:
  - Txn ID (top-left)
  - Buyer Name (bottom-left)
  - Total Amount (right)

#### Bill Preview (on tap)
- Shows:
  - Item list with quantities and prices
  - Total, discount, final amount
  - Date, time, buyer info

#### Search & Filter
- By:
  - Buyer Name
  - Phone Number
  - Txn ID
  - Date range

---

### 3.8 Settings

#### Stall Details
- Name
- Address
- Phone Number

#### Security
- Change App PIN
- Full App Reset:
  - Danger zone with text confirmation (`delete`)

#### Printer Settings
- Bluetooth MAC address
- Padding (top/bottom)
- Width
- **Test Print** functionality

#### Cloud Sync (Hidden)
- Logic pre-built, UI hidden by default
- Manual-trigger only
- Only visible if enabled later

#### Data Export
- Export CSV:
  - Transactions
  - Sales summary
  - Top items
- Saved to local device (Downloads folder)

---

## 4. 🗃️ Data Structure (Overview) (change if you have better)

| Table          | Fields                                                              |
|----------------|---------------------------------------------------------------------|
| Categories     | id, name                                                            |
| Items          | id, name, price, category_id                                        |
| Transactions   | id, txn_id, buyer_name, buyer_phone, date_time, subtotal, discount, total |
| TransactionItems | id, transaction_id, item_id, quantity, unit_price, line_total     |
| Settings       | stall_name, address, phone, pin, printer_mac, padding_top, padding_bottom |

---

## 4. 📲 App Flow & UI/UX Structure

This section outlines the navigation and user flow between the screens. The UI is optimized for tablet usage with touch-friendly elements.

---

### 🔐 App Launch Flow

1. **App Starts**
2. User sees **PIN Lock Screen**
   - Input field + number pad
   - Default PIN: `1111`
   - Option to change PIN in settings
3. On successful PIN → lands on **Main Menu**

---

### 🏠 Main Menu (Landing Screen)

> Central hub with 5 actions.

**Screen Layout:**

- Big buttons:
  - [ Inventory ]
  - [ Billing ]
  - [ Dashboard ]
  - [ Past Transactions ]
- Top-right gear icon → [ Settings ]

---

### 🛒 Inventory Screen

1. User taps "Inventory" → navigates to Inventory screen
2. Two tabs or sections:
   - Categories: Add/Edit/Delete
   - Items: Add/Edit/Delete with name, price, category

> Simple, clean form-based inputs. No stock quantity or image.

---

### 💵 Billing Screen (Tile UI)

1. User taps "Billing" → lands on category-wise tile grid
2. UI Layout:
   - Top: Horizontal category list
   - Middle: Grid of item tiles
     - Rectangle tile shows item name & price
     - Tap tile to add item
     - ➕ and ➖ buttons on tile sides
     - Tap anywhere (except ➖) to increase
     - Border turns green, text fades when selected
   - Cart on side or bottom (depending on screen size)

3. Cart Features:
   - List of items: Name, Qty, Price, Line Total
   - Manual quantity input for any item
   - Discount: Apply by ₹ or %
   - Optional: Buyer Name and Phone fields

4. Actions:
   - **[Print Receipt]** → saves transaction and prints
   - **[Clear Cart]** → resets billing

---

### 🧾 Receipt Flow

On clicking [Print Receipt]:
1. Data sent to Bluetooth printer
2. Format includes:
   - Stall info
   - Buyer info (if entered)
   - Items in tabular format
   - Totals and Discount
3. Print using ESC/POS protocol
4. Transaction saved with auto-generated ID (`TXNYYYYMMDD-001`)

---

### 📊 Dashboard Screen

1. User taps "Dashboard"
2. Two tabs or toggle buttons:
   - **Sales Chart**
     - View as:
       - Last 30 Days (Day-wise bar chart)
       - Last 12 Months (Month-wise)
   - **Top Selling Items**
     - Filters: Today, Last 7/30 Days, Custom Range
     - Data: Item Name, Quantity, Revenue

---

### 📂 Past Transactions

1. User taps "Past Transactions"
2. Grouped by date
3. Each transaction shown as a **tile/row**:
   - Txn ID, Buyer Name, Total Amount
4. Tap tile → open **Bill Preview**
   - Same data as receipt, but not formatted for print
5. Search bar allows:
   - Txn ID
   - Buyer Name
   - Buyer Phone
   - Date filter

---

### ⚙️ Settings Screen

1. Stall Details:
   - Name, Address, Phone
2. Security:
   - Change PIN
   - Danger Zone: Reset App (type `delete` to confirm)
3. Printer Settings:
   - Set MAC Address
   - Padding Top / Bottom
   - Width setting
   - Test Print button
4. Export:
   - Export Transactions, Sales, Top Items to CSV
5. Cloud Sync:
   - Hidden by default
   - Button only appears if toggled (future use)

---

### 🔄 Optional App Flow Map (Simple)


[ PIN Lock ]
↓
[ Main Menu ]
├──→ Inventory
├──→ Billing
│       └──→ Print Receipt
├──→ Dashboard
├──→ Past Transactions
└──→ Settings
├──→ Printer Settings
├──→ Stall Info
├──→ Security
├──→ Export / Reset
└──→ (Hidden) Cloud Sync


---

## 📐 UI Design Guidelines

- Optimized for **tablet screens** (7–10")
- Minimalist, touch-friendly buttons and inputs
- Consistent font and layout
- Color indicators (e.g., green border for selected tiles)
- Tabbed/category navigation for filtering
- Large total numbers on cart and receipts
- Modal or floating card for bill previews
```


---
## 5. 🛠️ Technical Requirements

- **Platform**: Android Tablet (Java or Kotlin)
- **Storage**: Local SQLite DB
- **Bluetooth Printing**: ESC/POS over Bluetooth
- **Charts**: MPAndroidChart (or similar)
- **UI**: Native XML or Jetpack Compose

---

## 6. 🚫 Exclusions (Out of Scope)

- No automatic syncing or background cloud features
- No inventory/stock management
- No multi-user or roles
- No taxes or service charges
- No customer loyalty systems

---

## 7. 🕹️ Future-Proofing

- Sync-to-cloud logic can be revealed via UI toggle if needed
- Hidden “Sync” button only becomes active on demand
- Full cloud support can be added later without rebuilding app

---

## 8. 📌 Acceptance Criteria

- Fully functional offline, with persistent local data
- Receipt prints via Bluetooth ESC/POS printer correctly
- PIN lock works and can be updated
- Dashboard, billing, and past transactions meet design spec
- CSV export works
- Reset functionality is protected by confirmation
- UI is responsive and optimized for tablet use
- No crashes or freezing under normal use

---

