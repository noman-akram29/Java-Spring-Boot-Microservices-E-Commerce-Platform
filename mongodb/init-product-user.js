
// Create dedicated application user for product-service.
// Root is used only for bootstrap / admin tasks.
// authSource=admin is required because the user is created in the admin database.

db = db.getSiblingDB("admin");

db.createUser({
  user: "product_app",
  pwd: "ProdAppSecur3Passw0rd!",
  roles: [
    { role: "readWrite", db: "product_db" }
  ]
});

// Ensure the application database exists
db = db.getSiblingDB("product_db");
db.createCollection("_init");