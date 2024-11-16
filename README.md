# ExpenseIQ

ExpenseIQ is an intuitive expense tracking app designed to help users manage monthly expenses across various categories, such as Food, Shopping, Rent, and more. The app leverages features like SMS-based transaction detection and insightful analytics to offer an organized, user-friendly experience.

## Features

- **Category-based Expense Tracking**: Easily track expenses by categories.
- **Automated Transaction Detection**: Captures transactions directly from SMS notifications, helping users log expenses without manual input.
- **Pending Payments**: View and categorize unlogged expenses from SMS-detected transactions.
- **Insights and Analytics**: Visual graphs provide monthly spending comparisons and insights.
- **Category Management**: Allows adding, editing, and deleting expense categories.
- **Navigation Drawer and About Page**: Side navigation for quick access to features and app information.

## Tech Stack

- **Kotlin**: Main language for Android development.
- **Jetpack Compose**: Declarative UI framework.
- **Room Database**: Local database for managing expenses, categories, and transactions.
- **Material 3 Components**: Modern UI elements following Material Design 3.
- **Broadcast Receiver**: Listens for SMS messages to auto-detect debit transactions.

## Main Screens

- **Home Screen**: View categorized expenses, total monthly spending, and add new transactions.
- **Pending Payments**: Categorize transactions detected from SMS notifications.
- **Insights**: Graphical representation of monthly expenses.
- **Edit Categories**: Manage categories (add, edit, delete).
- **About Page**: View app version, icon, and credits.

## License

ExpenseIQ is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
