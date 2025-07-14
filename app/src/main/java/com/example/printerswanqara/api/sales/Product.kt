package com.example.printerswanqara.api.sales

// --- Product Model and Related Data Classes ---
data class Product(
    val id: String,
    val slug: String,
    val active: Boolean,
    val stock: Double,
    val stock_warehouse: Double,
    val product_type_id: String,
    val product_type_name: String,
    val product_type: ProductType,
    val img: String,
    val image_b64: String,
    val classification_id: String,
    val classification: Classification,
    val categories: List<Category>,
    val brand_id: String,
    val kardex: List<Inventory>,
    val amount: Double,
    val spent: Boolean? = null,
    val asociatedProduct: Product? = null,
    val taxes: List<Rate>,
    val all_discounts: List<AllDiscount>,
    val discount: Double,
    val discounts: List<AllDiscount>,
    val warehouse_inventory: WarehouseInventory,
    val latest_inventory_ledger: InventoryLedger,
    val full_price: Double? = null,
    val production_cost: Double? = null,
    val name : String,
    val price : Double,
)

data class ProductType(
    val id: String,
    val name: String
)

data class Classification(
    val id: String,
    val name: String
)

data class Category(
    val id: String,
    val name: String
)

data class Inventory(
    val id: String,
    val warehouse_id: String,
    val warehouse_name: String,
    val stock_warehouse: String,
    val stock_warehouse_value: String,
    val available_product_series: List<ProductSeries>?,
    val attribute_values: List<AttributeValue>?,
    val warehouse: Warehouse
)

data class ProductSeries(
    val created_at: String,
    val deleted_at: String?,
    val id: String,
    val observation: String?,
    val product_id: String,
    val status: String,
    val stock: Double,
    val updated_at: String,
    val value: String,
    val warehouse_id: String
)

data class AttributeValue(
    val value: String,
    val amount: Double,
    val product_attribute_type: String? = null,
    val product_attribute_id: String? = null,
    val additional_information: List<AdditionalInformation>? = null
)

data class AdditionalInformation(
    val value: String,
    val label: String
)

data class WarehouseInventory(
    val id: String,
    val date: String,
    val type: String,
    val initial: Double,
    val observation: String?,
    val product_id: String,
    val responsible: String,
    val source: String,
    val stock_cost_total: String,
    val stock_cost_unit: String,
    val stock_units: String,
    val stock_warehouse: String,
    val target: String,
    val total_cost: String,
    val unit_cost: String,
    val units: String,
    val warehouse_id: String
)

data class InventoryLedger(
    val warehouse_id: String,
    val stock_cost_unit: Double
)

data class Rate(
    val id: String,
    val name: String,
    val code: String,
    val rate: String,
    val description: String,
    val validity: String?,
    val active: Boolean,
    val tax_id: String,
    val rate_tax_id: String,
    val pivot: Pivot,
    val tax: Tax,
    val show_product_codes: Boolean
)

data class Pivot(
    val tenant_id: String,
    val rate_tax_id: String
)

data class Tax(
    val id: String,
    val rate: String? = null,
    val base: String? = null,
    val amount: String? = null,
    val name: String,
    val code: String,
    val active: Boolean,
    val deleted_at: String?
)

data class AllDiscount(
    val id: String,
    val active: Boolean,
    val name: String,
    val description: String,
    val type: String,
    val quantity: Double,
    val discount: String,
    val application_method: String,
    val starting_date: String,
    val ending_date: String,
    val starting_time: String,
    val ending_time: String,
    val weekdays: List<String>,
    val filter: String
)

data class Img(
    val id: String?,
    val name: String?,
    val observation: String?,
    val default: Boolean,
    val image_b64: String?,
    val full_path: String?,
    val thumbnail_full_path: String?
)

