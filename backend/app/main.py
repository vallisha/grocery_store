from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from datetime import date, timedelta
from typing import Optional
import os

from .database import engine, get_db, Base
from .models import GroceryItem, Category, Location, BarcodeProduct
from .schemas import GroceryItemCreate, GroceryItemUpdate, GroceryItemResponse, CategoryCreate, CategoryResponse, LocationCreate, LocationResponse, BarcodeProductResponse

DEFAULT_CATEGORIES = [
    "rice", "cereals & pulses", "oil", "snacks", "sugar", "sweets",
    "powders", "spices", "dairy", "vegetables", "fruits", "beverages", "meat", "other",
]
DEFAULT_LOCATIONS = ["fridge", "freezer", "pantry", "shelf-1", "shelf-2", "shelf-3"]

app = FastAPI(title="Grocery Warehouse API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

STATIC_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "static")


@app.get("/")
def serve_frontend():
    return FileResponse(os.path.join(STATIC_DIR, "index.html"))


@app.get("/static/{file_path:path}")
def serve_static(file_path: str):
    return FileResponse(os.path.join(STATIC_DIR, file_path))


@app.on_event("startup")
def startup():
    Base.metadata.create_all(bind=engine)
    from .database import SessionLocal
    db = SessionLocal()
    if db.query(Category).count() == 0:
        for name in DEFAULT_CATEGORIES:
            db.add(Category(name=name))
        db.commit()
    if db.query(Location).count() == 0:
        for name in DEFAULT_LOCATIONS:
            db.add(Location(name=name))
        db.commit()
    db.close()


# --- Category endpoints ---

@app.get("/categories/", response_model=list[CategoryResponse])
def list_categories(db: Session = Depends(get_db)):
    return db.query(Category).order_by(Category.name).all()


@app.post("/categories/", response_model=CategoryResponse, status_code=201)
def create_category(cat: CategoryCreate, db: Session = Depends(get_db)):
    if db.query(Category).filter(Category.name == cat.name.lower().strip()).first():
        raise HTTPException(400, "Category already exists")
    c = Category(name=cat.name.lower().strip())
    db.add(c)
    db.commit()
    db.refresh(c)
    return c


@app.delete("/categories/{category_id}", status_code=204)
def delete_category(category_id: int, db: Session = Depends(get_db)):
    c = db.query(Category).filter(Category.id == category_id).first()
    if not c:
        raise HTTPException(404, "Category not found")
    db.delete(c)
    db.commit()


# --- Location endpoints ---

@app.get("/locations/", response_model=list[LocationResponse])
def list_locations(db: Session = Depends(get_db)):
    return db.query(Location).order_by(Location.name).all()


@app.post("/locations/", response_model=LocationResponse, status_code=201)
def create_location(loc: LocationCreate, db: Session = Depends(get_db)):
    if db.query(Location).filter(Location.name == loc.name.lower().strip()).first():
        raise HTTPException(400, "Location already exists")
    l = Location(name=loc.name.lower().strip())
    db.add(l)
    db.commit()
    db.refresh(l)
    return l


@app.delete("/locations/{location_id}", status_code=204)
def delete_location(location_id: int, db: Session = Depends(get_db)):
    l = db.query(Location).filter(Location.id == location_id).first()
    if not l:
        raise HTTPException(404, "Location not found")
    db.delete(l)
    db.commit()


# --- Barcode lookup (checks local DB first, then Open Food Facts) ---

@app.get("/barcode/{barcode}")
def lookup_barcode(barcode: str, db: Session = Depends(get_db)):
    import urllib.request, json
    # 1. Check local DB first (instant, learned from your past entries)
    local = db.query(BarcodeProduct).filter(BarcodeProduct.barcode == barcode).first()
    if local:
        return {"source": "local", "name": local.name, "category": local.category, "unit": local.unit}
    # 2. Try Open Food Facts (good for international + some Indian products)
    try:
        url = f"https://world.openfoodfacts.org/api/v0/product/{barcode}.json"
        req = urllib.request.Request(url, headers={"User-Agent": "GroceryWarehouse/1.0"})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read())
        if data.get("status") == 1 and data.get("product"):
            p = data["product"]
            name = p.get("product_name") or p.get("product_name_en") or ""
            if name:
                return {"source": "openfoodfacts", "name": name, "category": None, "unit": None}
    except Exception:
        pass
    # 3. Try UPC Item DB (has some Indian products)
    try:
        url = f"https://api.upcitemdb.com/prod/trial/lookup?upc={barcode}"
        req = urllib.request.Request(url, headers={"User-Agent": "GroceryWarehouse/1.0"})
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read())
        items = data.get("items", [])
        if items and items[0].get("title"):
            return {"source": "upcitemdb", "name": items[0]["title"], "category": None, "unit": None}
    except Exception:
        pass
    return {"source": "not_found", "name": None, "category": None, "unit": None}


@app.post("/items/", response_model=GroceryItemResponse, status_code=201)
def create_item(item: GroceryItemCreate, db: Session = Depends(get_db)):
    db_item = GroceryItem(**item.model_dump())
    db.add(db_item)
    db.commit()
    db.refresh(db_item)
    # Learn barcode → product mapping for future scans
    if item.barcode:
        existing = db.query(BarcodeProduct).filter(BarcodeProduct.barcode == item.barcode).first()
        if not existing:
            db.add(BarcodeProduct(barcode=item.barcode, name=item.name, category=item.category, unit=item.unit))
            db.commit()
    return db_item


@app.get("/items/", response_model=list[GroceryItemResponse])
def list_items(
    search: Optional[str] = None,
    category: Optional[str] = None,
    location: Optional[str] = None,
    expired_only: bool = False,
    db: Session = Depends(get_db),
):
    q = db.query(GroceryItem)
    if search:
        q = q.filter(GroceryItem.name.ilike(f"%{search}%"))
    if category:
        q = q.filter(GroceryItem.category == category)
    if location:
        q = q.filter(GroceryItem.location == location)
    if expired_only:
        q = q.filter(GroceryItem.expiry_date != None, GroceryItem.expiry_date < date.today())
    return q.order_by(GroceryItem.expiry_date.asc()).all()


@app.get("/items/expiring-soon/", response_model=list[GroceryItemResponse])
def expiring_soon(days: int = Query(default=3, ge=1), db: Session = Depends(get_db)):
    deadline = date.today() + timedelta(days=days)
    return db.query(GroceryItem).filter(
        GroceryItem.expiry_date != None,
        GroceryItem.expiry_date <= deadline,
        GroceryItem.expiry_date >= date.today(),
    ).all()


@app.get("/stats/")
def stats(db: Session = Depends(get_db)):
    items = db.query(GroceryItem).all()
    today = date.today()
    soon = today + timedelta(days=3)
    by_category, by_location = {}, {}
    expired = expiring = 0
    for i in items:
        by_category[i.category] = by_category.get(i.category, 0) + 1
        by_location[i.location] = by_location.get(i.location, 0) + 1
        if i.expiry_date:
            if i.expiry_date < today:
                expired += 1
            elif i.expiry_date <= soon:
                expiring += 1
    return {
        "total_items": len(items),
        "expired_count": expired,
        "expiring_soon_count": expiring,
        "by_category": by_category,
        "by_location": by_location,
    }


@app.get("/items/{item_id}", response_model=GroceryItemResponse)
def get_item(item_id: int, db: Session = Depends(get_db)):
    item = db.query(GroceryItem).filter(GroceryItem.id == item_id).first()
    if not item:
        raise HTTPException(404, "Item not found")
    return item


@app.put("/items/{item_id}", response_model=GroceryItemResponse)
def update_item(item_id: int, updates: GroceryItemUpdate, db: Session = Depends(get_db)):
    item = db.query(GroceryItem).filter(GroceryItem.id == item_id).first()
    if not item:
        raise HTTPException(404, "Item not found")
    for k, v in updates.model_dump(exclude_unset=True).items():
        setattr(item, k, v)
    db.commit()
    db.refresh(item)
    return item


@app.post("/items/{item_id}/consume")
def consume_item(item_id: int, db: Session = Depends(get_db)):
    item = db.query(GroceryItem).filter(GroceryItem.id == item_id).first()
    if not item:
        raise HTTPException(404, "Item not found")
    item.quantity -= 1
    if item.quantity <= 0:
        db.delete(item)
        db.commit()
        return {"deleted": True}
    db.commit()
    db.refresh(item)
    return item

@app.delete("/items/{item_id}")
def delete_item(item_id: int, db: Session = Depends(get_db)):
    item = db.query(GroceryItem).filter(GroceryItem.id == item_id).first()
    if not item:
        raise HTTPException(404, "Item not found")
    db.delete(item)
    db.commit()
