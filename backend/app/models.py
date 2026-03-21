from sqlalchemy import Column, Integer, String, Float, Date, DateTime
from datetime import datetime
from .database import Base


class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    name = Column(String, nullable=False, unique=True)


class Location(Base):
    __tablename__ = "locations"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    name = Column(String, nullable=False, unique=True)


class GroceryItem(Base):
    __tablename__ = "grocery_items"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    name = Column(String, nullable=False)
    quantity = Column(Float, nullable=False)
    unit = Column(String, nullable=False)
    category = Column(String, nullable=False)
    location = Column(String, nullable=False)
    purchase_date = Column(Date, nullable=False)
    expiry_date = Column(Date, nullable=True)
    notes = Column(String, nullable=True)
    barcode = Column(String, nullable=True, index=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class BarcodeProduct(Base):
    """Local barcode-to-product mapping. Learned from user entries."""
    __tablename__ = "barcode_products"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    barcode = Column(String, nullable=False, unique=True, index=True)
    name = Column(String, nullable=False)
    category = Column(String, nullable=True)
    unit = Column(String, nullable=True)
