# 🏪 Grocery Warehouse

A home grocery inventory management app to track what items you have, where they are stored, their expiry dates, and more.

## Features

- ➕ Add/edit/delete grocery items with name, quantity, category, location, dates
- 📷 Barcode scanning with auto-lookup (Open Food Facts + self-learning local DB)
- 🔍 Search and filter by category/location
- 📊 Dashboard with stats and breakdowns
- ⚙️ Dynamic categories and locations management
- ⏰ Expiry date tracking with color-coded alerts
- 🎨 Mobile-friendly responsive UI

## Tech Stack

- **Backend**: Python + FastAPI + SQLite
- **Web Frontend**: HTML/CSS/JS (single page app)
- **Android Frontend**: Kotlin + Jetpack Compose (for future APK build)

## Quick Start

```bash
cd backend
pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Open `http://localhost:8000` in your browser.

## Project Structure

```
grocery_store/
├── backend/           # Python FastAPI backend + web UI
│   ├── app/
│   │   ├── main.py        # API endpoints
│   │   ├── models.py      # Database models
│   │   ├── schemas.py     # Pydantic schemas
│   │   └── database.py    # SQLite setup
│   ├── static/
│   │   ├── index.html     # Web frontend
│   │   └── app.js         # Frontend logic
│   └── requirements.txt
└── android/           # Native Android app (build with Android Studio)
```
