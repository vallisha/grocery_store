from pydantic import BaseModel
from datetime import date, datetime
from typing import Optional


class GroceryItemCreate(BaseModel):
    name: str
    quantity: float
    unit: str
    category: str
    location: str
    purchase_date: date
    expiry_date: Optional[date] = None
    notes: Optional[str] = None
    barcode: Optional[str] = None


class GroceryItemUpdate(BaseModel):
    name: Optional[str] = None
    quantity: Optional[float] = None
    unit: Optional[str] = None
    category: Optional[str] = None
    location: Optional[str] = None
    purchase_date: Optional[date] = None
    expiry_date: Optional[date] = None
    notes: Optional[str] = None


class GroceryItemResponse(BaseModel):
    id: int
    name: str
    quantity: float
    unit: str
    category: str
    location: str
    purchase_date: date
    expiry_date: Optional[date]
    notes: Optional[str]
    barcode: Optional[str]
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class CategoryCreate(BaseModel):
    name: str


class CategoryResponse(BaseModel):
    id: int
    name: str

    model_config = {"from_attributes": True}


LocationCreate = CategoryCreate
LocationResponse = CategoryResponse


class BarcodeProductResponse(BaseModel):
    barcode: str
    name: str
    category: Optional[str]
    unit: Optional[str]

    model_config = {"from_attributes": True}
