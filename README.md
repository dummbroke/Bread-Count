# Bread Count - Bakery Inventory Management System

## 📋 Overview

**Bread Count** is a comprehensive Android inventory management application designed specifically for bakeries and small food businesses. The application provides real-time inventory tracking, sales transaction management, and detailed reporting capabilities with Firebase backend integration.

## ✨ Key Features

### 🔐 Authentication System
- **Email/Password Authentication** via Firebase Auth
- **Google Sign-In** integration with credential management
- **Secure Session Management** with automatic authentication state handling
- **Account Verification** with real-time authentication state monitoring

### 📦 Inventory Management
- **Multi-Category Support**: Manage three distinct product categories:
  - **Display Bread**: Fresh bread items displayed in store
  - **Display Beverages**: Drinks and beverage products
  - **Delivery Bread**: Bread items prepared for delivery orders
- **CRUD Operations**: Complete Create, Read, Update, Delete functionality
- **Real-time Synchronization**: Instant updates across all devices using Firebase Firestore
- **Quantity Tracking**: Monitor stock levels with automatic inventory adjustments

### 📊 Dashboard & Real-Time Overview
- **Live Inventory Display**: Real-time view of all categories with current stock levels
- **Quick Transaction Recording**: Built-in sales recording interface on main dashboard
- **Category-wise Organization**: Organized display with separate RecyclerViews for each category
- **Instant Updates**: Automatic refresh when inventory changes occur

### 💰 Transaction Management
- **Sales Recording**: Record individual sales transactions with detailed information
- **Owner vs Customer Transactions**: Distinguish between owner consumption and customer sales
- **Automatic Inventory Updates**: Real-time inventory deduction upon transaction recording
- **Transaction History**: Complete chronological record of all sales activities
- **Daily Filtering**: View transactions by specific dates or categories

### 📈 Reporting & Analytics
- **CSV Export Functionality**: Generate detailed daily sales reports
- **Sales Categorization**: Separate reporting for different product categories
- **Owner Transaction Tracking**: Dedicated section for owner/internal consumption
- **Total Sales Calculation**: Automatic calculation of daily sales totals (excluding owner transactions)
- **Transaction Initials**: Smart name-to-initials conversion for compact reporting

### 🎨 User Interface
- **Material Design 3**: Modern, intuitive interface following Google's design guidelines
- **Bottom Navigation**: Easy access to Dashboard, Inventory, and Transaction screens
- **Drawer Navigation**: Additional menu options including logout functionality
- **Responsive Design**: Optimized for various Android screen sizes
- **Custom Spinners & Dialogs**: Professionally designed input components

## 🏗️ Technical Architecture

### Architecture Pattern
- **MVVM (Model-View-ViewModel)**: Clean separation of concerns with reactive programming
- **Repository Pattern**: Centralized data access layer for Firebase operations
- **Single Activity Architecture**: Fragment-based navigation with single MainActivity

### Technology Stack
- **Language**: Kotlin (100%)
- **UI Framework**: Android Views with ViewBinding
- **Backend**: Firebase Firestore for real-time database
- **Authentication**: Firebase Authentication
- **Reactive Programming**: Kotlin Coroutines & Flow
- **Navigation**: Android Navigation Component with Bottom Navigation
- **Design**: Material Design 3 components

### Dependencies & Libraries
```gradle
- Android SDK: API 24-35 (Android 7.0 - Android 15)
- Kotlin: 2.0.0
- Firebase BOM: 32.7.2
- Material Components: 1.12.0
- Navigation Components: 2.8.9
- Lifecycle: 2.8.7
- RecyclerView: 1.4.0
- Fragment KTX: 1.8.6
```

## 📱 Application Screens

### 1. Authentication Flow
- **Sign In Screen**: Email/password and Google Sign-In options
- **Sign Up Screen**: New account creation with form validation
- **Automatic Redirection**: Seamless navigation between auth and main app

### 2. Dashboard Screen
- **Category Overview**: Real-time display of all inventory categories
- **Transaction Recorder**: Embedded interface for quick sales recording
- **Dynamic Updates**: Live refresh of inventory quantities
- **Interactive Controls**: Plus/minus buttons for quantity adjustment

### 3. Inventory Management Screen
- **Category Filtering**: Switch between different product categories
- **Add New Items**: Dialog-based item creation with validation
- **Edit Existing Items**: In-place editing of item details
- **Delete Items**: Confirmation-based item removal
- **Comprehensive Item Display**: Name, price, quantity, and category information

### 4. Transaction History Screen
- **Daily Transaction View**: Filter by date and category
- **Transaction Details**: Complete information including timestamps
- **Export Functionality**: Generate and share CSV reports
- **Sales Analytics**: Real-time calculation of daily totals

## 🔥 Firebase Integration

### Firestore Database Structure
```
users/
├── {userId}/
│   ├── display_bread/
│   │   └── {itemId} → {name, price, quantity, category}
│   ├── display_beverages/
│   │   └── {itemId} → {name, price, quantity, category}
│   ├── delivery_bread/
│   │   └── {itemId} → {name, price, quantity, category}
│   └── transactions/
│       └── {transactionId} → {itemId, itemName, category, price, quantity, totalAmount, isOwner, ownerName, timestamp}
```

### Security Features
- **User-based Data Isolation**: Each user's data is completely separated
- **Authentication Required**: All operations require valid Firebase authentication
- **Real-time Security Rules**: Firestore rules ensure data privacy and integrity

## 🚀 Core Functionalities

### Inventory Operations
1. **Add Item**: Create new inventory items with name, price, category, and initial quantity
2. **View Items**: Real-time display of current inventory with category filtering
3. **Update Item**: Edit existing item details including quantity adjustments
4. **Delete Item**: Remove items with confirmation dialog
5. **Category Management**: Organize items across three predefined categories

### Transaction Processing
1. **Record Sale**: Process customer purchases with automatic inventory deduction
2. **Owner Transactions**: Track internal consumption separate from sales
3. **Quantity Validation**: Ensure sufficient inventory before processing transactions
4. **Real-time Updates**: Immediate reflection of changes across all screens

### Data Export & Reporting
1. **CSV Generation**: Create detailed reports with transaction breakdowns
2. **Category Segregation**: Separate sections for each product category
3. **Sales Totals**: Automatic calculation of category and overall totals
4. **Owner Activity Tracking**: Dedicated reporting for internal transactions

## 🔧 Development Features

### State Management
- **StateFlow**: Reactive state management for UI updates
- **UI State Classes**: Structured state representation for each screen
- **Loading States**: Proper handling of loading, success, and error states

### Error Handling
- **Comprehensive Exception Handling**: Graceful error management throughout the app
- **User Feedback**: Toast messages and dialog alerts for user communication
- **Network Error Recovery**: Automatic retry mechanisms for Firebase operations

### Performance Optimizations
- **Lazy Loading**: Efficient data loading with pagination support
- **RecyclerView Optimization**: DiffUtil for efficient list updates
- **Memory Management**: Proper lifecycle handling and resource cleanup

## 📋 Business Logic

### Inventory Management Rules
- Automatic quantity reduction upon sales transaction
- Prevention of negative inventory levels
- Real-time synchronization across multiple devices
- Category-based organization for better inventory control

### Transaction Rules
- Owner transactions don't count toward sales totals
- All transactions require authentication
- Automatic timestamp recording for audit trails
- Quantity validation before transaction processing

### Reporting Logic
- Daily transaction filtering by date
- Category-wise sales breakdown
- Owner vs customer transaction separation
- CSV format optimized for spreadsheet applications

## 🛡️ Security & Privacy

### Data Protection
- Firebase Authentication ensures secure user access
- User data isolation prevents cross-contamination
- Secure network communication via HTTPS
- No sensitive data stored locally

### Permission Management
- Internet access for Firebase communication
- Network state monitoring for connectivity awareness
- No invasive permissions required

## 🔄 Real-time Features

### Live Data Synchronization
- Instant inventory updates across all connected devices
- Real-time transaction recording and display
- Automatic UI refresh when data changes
- Offline support with automatic sync when reconnected

### Reactive Programming
- Flow-based data streams for efficient updates
- StateFlow for UI state management
- Coroutines for asynchronous operations
- LiveData integration for lifecycle-aware updates

---

*Bread Count is designed to be a comprehensive, user-friendly solution for bakery inventory management, combining modern Android development practices with powerful Firebase backend capabilities.* 
