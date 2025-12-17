# 🎨 Visual UI Guide - Dashboard Refactoring

## Quick Reference for Testing & Review

---

## 📸 Screenshot Guide (What to Look For)

### 1️⃣ HEADER AREA - Top Section

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  Dashboard Dược Sĩ - Vận Hành                                                          │
│                                                                                         │
│  [🔍 Tìm kiếm sản phẩm...] [Tìm]    ┌──────────────────┐  🔔  [Đóng ca]             │
│                                      │ Mã Ca: SH-001    │                              │
│                                      │ Tiền: 5,234,500₫ │                              │
│  Ngày: 18/12/2025        [Làm mới]  └──────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

**Key Elements to Verify:**
- ✓ Title "Dashboard Dược Sĩ - Vận Hành" on the left
- ✓ Search box in center with magnifying glass icon
- ✓ **Shift Widget** (light blue box) showing:
  - Shift ID
  - Current cash amount
- ✓ **Bell Icon (🔔)** - should be clickable
- ✓ **[Đóng ca]** button on far right - RED color

---

### 2️⃣ EXPIRING PRODUCTS TABLE - With Row Colors

```
┌─ CẢNH BÁO: Thuốc Sắp Hết Hạn ─────────────────────────── 3 lô hàng ─┐
│                                                                        │
│  Mã lô    │ Tên sản phẩm      │ SL │ Hạn SD     │ Còn lại  │ Mức độ │ Thao tác     │
├───────────────────────────────────────────────────────────────────────────────────────┤
│🔴 LO-001  │ Aspirin 500mg     │ 50 │ 15/01/2026 │ 28 ngày  │ NGUY!  │ [Copy ID]    │
│  ← ENTIRE ROW HAS LIGHT RED BACKGROUND (255, 200, 200)                              │
├───────────────────────────────────────────────────────────────────────────────────────┤
│🟡 LO-002  │ Vitamin C 1000mg  │ 30 │ 20/02/2026 │ 64 ngày  │ CAO    │ [Copy ID]    │
│  ← ENTIRE ROW HAS LIGHT YELLOW BACKGROUND (255, 255, 200)                           │
├───────────────────────────────────────────────────────────────────────────────────────┤
│  LO-003  │ Paracetamol 500mg │ 20 │ 15/04/2026 │ 118 ngày │ TB     │ [Copy ID]    │
│  ← NORMAL WHITE BACKGROUND                                                           │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

**What to Test:**
1. Add product lot with expiry date in 25 days → Should show RED row
2. Add product lot with expiry date in 60 days → Should show YELLOW row
3. Add product lot with expiry date in 100 days → Should show WHITE row
4. Click [Copy ID] button → Clipboard should contain batch number
5. Click [Copy ID] → Should see popup: "Đã copy mã lô: LO-XXX"

**Color Rules:**
- **< 30 days** = 🔴 **RGB(255, 200, 200)** - Light Red
- **30-90 days** = 🟡 **RGB(255, 255, 200)** - Light Yellow  
- **> 90 days** = ⚪ **White**

---

### 3️⃣ PROMOTIONS TABLE - With Conditions

```
┌─ Khuyến Mãi Đang Áp Dụng ───────────────────────────── 4 chương trình ─┐
│                                                                          │
│  Mã KM    │ Tên KM        │ Ngày BĐ  │ Ngày KT  │ Điều kiện áp dụng    │
├──────────────────────────────────────────────────────────────────────────┤
│  PRM-001  │ Flash Sale    │ 01/12/25 │ 31/12/25 │ Hóa đơn ≥ 500,000 ₫  │
│  PRM-002  │ Combo Deal    │ 15/12/25 │ 20/12/25 │ Mua Paracetamol ≥ 2  │
│  PRM-003  │ Premium Tier  │ 01/12/25 │ 31/01/26 │ Hóa đơn từ 1,000,000 │
│           │               │          │          │ ₫ - 2,000,000 ₫      │
│  PRM-004  │ Multi Reward  │ 10/12/25 │ 25/12/25 │ Hóa đơn ≥ 500,000 ₫; │
│           │               │          │          │ Mua Aspirin...       │
└──────────────────────────────────────────────────────────────────────────┘
```

**Before vs After:**

❌ **BEFORE** (Description column):
```
│ Mô tả                                    │
│ Giảm giá 20% cho đơn hàng trên 500k     │
│ Mua 2 tặng 1 cho Paracetamol            │
```

✅ **AFTER** (Condition column):
```
│ Điều kiện áp dụng                        │
│ Hóa đơn ≥ 500,000 ₫                      │
│ Mua Paracetamol ≥ 2 sản phẩm             │
```

**Condition Format Examples:**

| Condition Type | Display Format | Example |
|----------------|----------------|---------|
| Order Subtotal | `Hóa đơn [symbol] [amount] ₫` | `Hóa đơn ≥ 500,000 ₫` |
| Order Between | `Hóa đơn từ [min] ₫ - [max] ₫` | `Hóa đơn từ 1M ₫ - 2M ₫` |
| Product Quantity | `Mua [product] [symbol] [qty] sản phẩm` | `Mua Aspirin ≥ 3 sản phẩm` |
| Product Specific | `Sản phẩm: [product]` | `Sản phẩm: Vitamin C` |
| Multiple | `[Cond1]; [Cond2]...` | `Hóa đơn ≥ 500k; Mua...` |

---

## 🔔 Notification Popup

**When Bell Icon Clicked:**

```
┌──────────────────────────────────────────┐
│    📊 THÔNG BÁO HỆ THỐNG                 │
│                                          │
│  🔴 Thuốc sắp hết hàng: 5 sản phẩm      │
│  🟡 Thuốc sắp hết hạn: 12 lô hàng       │
│                                          │
│  Vui lòng kiểm tra và xử lý kịp thời!   │
│                                          │
│                              [OK]        │
└──────────────────────────────────────────┘
```

**Test Steps:**
1. Click 🔔 bell icon in header
2. Verify popup shows correct counts
3. Click [OK] to dismiss

---

## 🎯 Interactive Elements Test Guide

### A. Shift Widget Interaction

**Scenario 1: No Shift Open**
```
┌──────────────────┐
│ Mã Ca: Chưa mở ca│  ← Gray text
│ Tiền: ---        │  ← Disabled
└──────────────────┘
[Đóng ca]  ← DISABLED (grayed out)
```

**Scenario 2: Shift Open**
```
┌──────────────────┐
│ Mã Ca: SH-12345  │  ← Blue bold text
│ Tiền: 5,234,500₫ │  ← Green bold text
└──────────────────┘
[Đóng ca]  ← ENABLED (red color)
```

**Test Flow:**
1. Start → No shift → Widget shows "Chưa mở ca"
2. Open shift → Widget updates → Shows shift ID and cash
3. Make sale → Click [Làm mới] → Cash amount updates
4. Click [Đóng ca] → Dialog opens → After close → Shows "Chưa mở ca"

---

### B. Copy Batch Number Feature

**Visual Flow:**
```
1. User sees expiring product:
   ┌────────────────────────────────────────┐
   │ LO-12345 │ Aspirin │ ... │ [Copy ID] │
   └────────────────────────────────────────┘
          ↓
2. User clicks [Copy ID]
          ↓
3. Popup appears:
   ┌─────────────────────────────┐
   │ ℹ️ Đã copy mã lô: LO-12345  │
   │              [OK]            │
   └─────────────────────────────┘
          ↓
4. User can paste: LO-12345 ✓
```

**Test:**
1. Click [Copy ID] button
2. Open Notepad
3. Press Ctrl+V
4. Verify batch number appears

---

### C. Search Bar (Future Implementation)

**Current State:**
```
[🔍 Tìm kiếm sản phẩm, khuyến mãi...] [Tìm]
 ↑ Input field (functional)          ↑ Button (not yet implemented)
```

**Note:** Search functionality is UI-only in current version. Backend implementation pending.

---

## 🎨 Color Palette Reference

### Header Colors
```
Title:        AppColors.PRIMARY (Blue)
Search:       AppColors.SECONDARY border (Teal)
Shift Widget: RGB(240, 248, 255) - Alice Blue background
              AppColors.SECONDARY - Border
Bell Icon:    AppColors.WARNING (Orange/Yellow)
Close Shift:  AppColors.DANGER (Red)
Refresh Btn:  AppColors.SECONDARY (Teal)
```

### Table Colors
```
Header Row:   AppColors.SECONDARY background, White text
Table Grid:   AppColors.BACKGROUND (Light gray)

Expiring Rows:
  Danger:     RGB(255, 200, 200) - Light Red
  Warning:    RGB(255, 255, 200) - Light Yellow
  Normal:     White

Low Stock:
  Critical:   AppColors.DANGER (Red text)
  Warning:    AppColors.WARNING (Orange text)

Promotions:
  Header:     AppColors.SUCCESS (Green)
```

### Status Indicators
```
Shift ID:     AppColors.PRIMARY (Blue)
Cash Amount:  AppColors.SUCCESS (Green)
Level Tags:
  NGUY HIỂM:  White text, AppColors.DANGER background
  CAO:        White text, AppColors.WARNING background
  TRUNG BÌNH: Black text, AppColors.LIGHT background
```

---

## 📏 Layout Dimensions

### Header
```
Total Height: ~120px
Top Section:  ~60px
  - Title: 28px font
  - Search: 35px height
  - Shift Widget: ~50px height
  - Bell Icon: 24px font
  - Close Button: 40px height
Bottom Section: ~40px
  - Date: 16px font
  - Refresh: 40px height
```

### Tables
```
Each Table:
  Height: 180px (scrollable)
  Row Height: 30px
  Header Height: 35px
  
Column Widths (Expiring Table):
  Mã lô: 100px
  Tên SP: 200px
  SL: 80px
  Hạn SD: 100px
  Còn lại: 80px
  Mức độ: 100px
  Thao tác: 100px

Column Widths (Promotion Table):
  Mã KM: 80px
  Tên KM: 200px
  Ngày BĐ: 100px
  Ngày KT: 100px
  Điều kiện: 300px
```

---

## ✅ Acceptance Criteria Checklist

### Header Area
- [ ] Shift widget visible in top-right
- [ ] Shift ID displays when shift open
- [ ] Current cash updates on refresh
- [ ] "Chưa mở ca" shows when no shift
- [ ] Bell icon is clickable
- [ ] Notification popup shows correct counts
- [ ] Search bar is visible and styled
- [ ] Close shift button is red and on far right
- [ ] Close shift button disabled when no shift
- [ ] Close shift opens dialog correctly

### Expiring Table
- [ ] Table has 7 columns (including Thao tác)
- [ ] Products < 30 days have RED background
- [ ] Products 30-90 days have YELLOW background
- [ ] Products > 90 days have WHITE background
- [ ] Row colors span entire row
- [ ] Action column shows [Copy ID] button
- [ ] Button is styled with secondary color
- [ ] Clicking button copies batch number
- [ ] Success message appears after copy
- [ ] Clipboard contains correct batch number

### Promotions Table
- [ ] Table has 5 columns
- [ ] "Điều kiện áp dụng" column present
- [ ] Order conditions show "Hóa đơn ≥ XXX ₫"
- [ ] Product conditions show "Mua [Product] ≥ X"
- [ ] Currency values have thousand separators
- [ ] Symbols display correctly (≥, ≤, etc.)
- [ ] Multiple conditions show "..."
- [ ] BETWEEN shows "từ X - Y"
- [ ] Column is wide enough (300px)

---

## 🐛 Common Issues & Solutions

### Issue 1: Shift Widget Shows "---"
**Cause**: Staff object not passed to dashboard
**Solution**: Verify TAB_Dashboard passes currentStaff to constructor

### Issue 2: Row Colors Not Showing
**Cause**: ExpiringRowRenderer not applied
**Solution**: Check `tblExpiringSoon.setDefaultRenderer(Object.class, new ExpiringRowRenderer())`

### Issue 3: Copy Button Doesn't Work
**Cause**: ButtonEditor not set on action column
**Solution**: Verify `getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(...))`

### Issue 4: Conditions Show "null"
**Cause**: Promotion conditions not loaded from database
**Solution**: Check BUS_Promotion.getConditionsByPromotionId() returns data

### Issue 5: Close Shift Always Disabled
**Cause**: loadShiftData() not called or currentStaff is null
**Solution**: Verify constructor calls loadShiftData() and staff is passed

---

## 📱 Responsive Behavior

### Full Width (> 1400px)
```
[Title]    [Search Bar]    [Shift Widget] [Bell] [Close]
```

### Medium Width (1000-1400px)
```
[Title]         [Search Bar]
                [Shift Widget] [Bell] [Close]
```

### Small Width (< 1000px)
```
[Title]
[Search Bar]
[Shift Widget] [Bell] [Close]
```

---

## 🎓 Training Guide for Users

### For Pharmacists:

**1. Understanding the Shift Widget**
- Top number = Your current shift ID
- Bottom number = Total cash in register (calculated by system)
- If it says "Chưa mở ca", you need to open a shift first

**2. Using the Color-Coded Warning System**
- 🔴 Red = URGENT: Handle these first (< 1 month left)
- 🟡 Yellow = Soon: Plan to process these (< 3 months)
- ⚪ White = OK: Normal inventory

**3. Quick Copy Feature**
- See a product expiring? Click [Copy ID]
- Paste it in product search to find quickly
- No more manual typing!

**4. Reading Promotion Conditions**
- "Hóa đơn ≥ 500,000 ₫" = Bill must be 500k or more
- "Mua X ≥ 2 sản phẩm" = Customer must buy 2 or more X
- Tell customers these conditions when recommending promotions

---

**Document Version**: 1.0  
**Last Updated**: December 18, 2025  
**For**: MediWOW Pharmacy Management System  
**Module**: Employee Dashboard (TAB_Dashboard_Pharmacist)

