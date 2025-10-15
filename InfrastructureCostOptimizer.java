import com.mongodb.client.*;
import org.bson.Document;
import java.util.*;

public class InfrastructureCostOptimizer {

    // --- MongoDB Connection ---
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void connect() {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("infraDB");
            System.out.println("✅ Connected to MongoDB!");
        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("🔒 MongoDB connection closed.");
        }
    }

    // --- Infrastructure Resource (model) ---
    static class InfrastructureResource {
        String resourceId;
        String resourceType;
        double costPerHour;
        double usageHours;
        double maxCapacity;
        double currentUsage;

        public InfrastructureResource(String resourceId, String resourceType,
                                      double costPerHour, double usageHours,
                                      double maxCapacity, double currentUsage) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
            this.costPerHour = costPerHour;
            this.usageHours = usageHours;
            this.maxCapacity = maxCapacity;
            this.currentUsage = currentUsage;
        }

        public double getTotalCost() {
            return costPerHour * usageHours;
        }

        public double getEfficiency() {
            if (maxCapacity <= 0 || costPerHour <= 0) return 0.0;
            return (currentUsage / maxCapacity) / costPerHour;
        }

        public Document toDocument() {
            return new Document("resourceId", resourceId)
                    .append("resourceType", resourceType)
                    .append("costPerHour", costPerHour)
                    .append("usageHours", usageHours)
                    .append("maxCapacity", maxCapacity)
                    .append("currentUsage", currentUsage);
        }

        @Override
        public String toString() {
            return resourceId + " (" + resourceType + ") cost/hr=" + costPerHour +
                    " usage=" + currentUsage + "/" + maxCapacity;
        }
    }

    // --- Insert sample data ---
    public static void insertSampleData(MongoCollection<Document> collection) {
        List<InfrastructureResource> resources = List.of(
                new InfrastructureResource("SRV001", "Server", 10, 5, 100, 40),
                new InfrastructureResource("SRV002", "Database", 20, 6, 150, 75),
                new InfrastructureResource("SRV003", "Storage", 5, 8, 200, 100),
                new InfrastructureResource("SRV004", "API Gateway", 15, 3, 120, 30)
        );

        for (InfrastructureResource r : resources) {
            collection.insertOne(r.toDocument());
        }
        System.out.println("✅ Sample data inserted!");
    }

    // --- Analyze Cost Efficiency ---
    public static void analyze(MongoCollection<Document> collection) {
        List<InfrastructureResource> list = new ArrayList<>();
        double totalCost = 0;

        for (Document doc : collection.find()) {
            InfrastructureResource r = new InfrastructureResource(
                    doc.getString("resourceId"),
                    doc.getString("resourceType"),
                    doc.getDouble("costPerHour"),
                    doc.getDouble("usageHours"),
                    doc.getDouble("maxCapacity"),
                    doc.getDouble("currentUsage")
            );
            list.add(r);
            totalCost += r.getTotalCost();
        }

        list.sort(Comparator.comparingDouble(InfrastructureResource::getEfficiency));
        System.out.println("\n=== Cost Analysis Report ===");
        System.out.println("Total Resources: " + list.size());
        System.out.println("Total Cost: $" + totalCost);

        System.out.println("\n⚠️  Least Efficient Resources:");
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            System.out.println("- " + list.get(i) + " | Efficiency: " + list.get(i).getEfficiency());
        }
    }

    // --- Main ---
    public static void main(String[] args) {
        connect();
        MongoCollection<Document> collection = database.getCollection("infrastructure");

        // Clear old data
        collection.drop();

        // Insert and analyze
        insertSampleData(collection);
        analyze(collection);

        close();
    }
}
